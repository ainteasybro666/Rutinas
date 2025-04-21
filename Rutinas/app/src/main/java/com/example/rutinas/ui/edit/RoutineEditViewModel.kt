package com.example.rutinas.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import com.example.rutinas.ui.viewmodel.BaseViewModel
import com.example.rutinas.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    private val repository: RoutineRepository
) : ViewModel() {

    private val _currentRoutine = MutableStateFlow<Routine?>(null)
    val currentRoutine: StateFlow<Routine?> = _currentRoutine.asStateFlow()

    private val _actions = MutableStateFlow<List<Action>>(emptyList())
    val actions: StateFlow<List<Action>> = _actions.asStateFlow()

    private val _saveResult = MutableSharedFlow<Resource<Long>>()
    val saveResult: SharedFlow<Resource<Long>> = _saveResult.asSharedFlow()

    fun loadRoutine(uuid: String) {
        Timber.d("Cargando rutina con UUID: $uuid")
        viewModelScope.launch {
            val routine = repository.getRoutineByUuid(uuid)
            _currentRoutine.value = routine
            _actions.value = routine?.actions ?: emptyList()
            Timber.d("Rutina cargada: $routine")
        }
    }

    fun updateAction(updatedAction: Action) {
        Timber.d("Actualizando acción: $updatedAction")
        viewModelScope.launch {
            repository.updateAction(updatedAction)
            _actions.update { currentActions ->
                currentActions.map { action ->
                    if (action.uuid == updatedAction.uuid) updatedAction else action
                }
            }
            Timber.d("Acción actualizada en la lista: ${_actions.value}")
        }
    }

    //  No es necesario llamar a repository.updateActions aquí, ya que las acciones se guardan/actualizan individualmente.
    fun updateActions(newActions: List<Action>) {
        Timber.d("Actualizando lista de acciones (sin guardar en repositorio): $newActions")
        _actions.value = newActions
    }

    fun removeAction(action: Action) {
        Timber.d("Eliminando acción: $action")
        viewModelScope.launch {
            repository.deleteAction(action)
            _actions.update { it.filterNot { it.uuid == action.uuid } }
            Timber.d("Acción eliminada de la lista: ${_actions.value}")
        }
    }

    fun saveRoutine(name: String, triggers: List<Trigger>, actions: List<Action>) {
        Timber.d("Guardando rutina con nombre: $name, triggers: $triggers, acciones: $actions")
        viewModelScope.launch {
            // Validaciones de triggers
            for (trigger in triggers) {
                if (trigger.triggerType == "TIME") {
                    val frequency = trigger.data["frequency"] as? String
                    val daysOfWeek = trigger.data["daysOfWeek"] as? List<Int>
                    val dayOfMonth = trigger.data["dayOfMonth"] as? Int

                    if (frequency == "weekly" && (daysOfWeek == null || daysOfWeek.isEmpty())) {
                        _saveResult.emit(Resource.Error("Debes seleccionar al menos un día de la semana para un trigger semanal."))
                        return@launch
                    }

                    if (frequency == "monthly" && (dayOfMonth == null || dayOfMonth !in 1..31)) {
                        _saveResult.emit(Resource.Error("El día del mes debe estar entre 1 y 31 para un trigger mensual."))
                        return@launch
                    }
                }
            }

            try {
                val routine = _currentRoutine.value?.copy(
                    name = name,
                    triggers = triggers,
                    actions = actions
                ) ?: Routine(
                    name = name,
                    uuid = "",
                    triggers = triggers,
                    actions = actions,
                    createdDate = LocalDateTime.now()
                )
                val result = repository.saveRoutine(routine)
                if (result is Resource.Success) {
                    _currentRoutine.value = routine.copy(id = result.data)
                    Timber.d("Rutina guardada con éxito, ID: ${result.data}")
                } else if (result is Resource.Error) {
                    Timber.e("Error al guardar rutina: ${result.message}")
                }
                _saveResult.emit(result)
            } catch (e: Exception) {
                Timber.e("Error inesperado al guardar rutina: ${e.message}")
                _saveResult.emit(Resource.Error(e.message ?: "Error desconocido"))
            }
        }
    }

    //  Nueva función para agregar un trigger.  Podría ser útil en el futuro si permitimos agregar triggers directamente desde el ViewModel.
    fun addTrigger(trigger: Trigger) {
        Timber.d("Agregando trigger: $trigger")
        _currentRoutine.update { currentRoutine ->
            currentRoutine?.copy(triggers = currentRoutine.triggers + trigger)
        }
        Timber.d("Trigger agregado, lista de triggers actualizada: ${_currentRoutine.value?.triggers}")
    }
}