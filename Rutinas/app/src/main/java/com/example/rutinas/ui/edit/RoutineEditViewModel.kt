package com.example.rutinas.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
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
import java.util.UUID // Import UUID
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

    private var isNewRoutine: Boolean = true // Flag to track if it's a new routine

    // Load routine based on UUID, or initialize for a new routine
    fun loadRoutine(uuid: String?) { // Acepta String?
        if (uuid == null) {
            // Creating a new routine
            isNewRoutine = true
            val newUuid = UUID.randomUUID().toString()
            _currentRoutine.value = Routine(id = 0, uuid = newUuid, name = "", triggers = emptyList(), actions = emptyList())
            _actions.value = emptyList() // Initial state for a new routine
            Timber.d("Initializing ViewModel for new routine with UUID: $newUuid")
        } else {
            // Editing existing routine
            isNewRoutine = false
            Timber.d("Loading routine with UUID: $uuid")
            viewModelScope.launch {
                val routine = repository.getRoutineByUuid(uuid)
                _currentRoutine.value = routine
                _actions.value = routine?.actions ?: emptyList()
                Timber.d("Rutina cargada: $routine")
            }
        }
    }

    fun updateAction(updatedAction: Action) {
        Timber.d("updateAction called with: $updatedAction")
        Timber.d("updateAction: Current actions before update: ${_actions.value}")
        viewModelScope.launch {
            _actions.update { currentActions ->
                Timber.d("updateAction: Inside _actions.update. Current actions: $currentActions")
                val newList = currentActions.map { action ->
                    if (action.uuid == updatedAction.uuid) {
                        Timber.d("updateAction: Found action with matching UUID: ${action.uuid}. Replacing with: $updatedAction")
                        updatedAction
                    } else {
                        action
                    }
                }
                Timber.d("updateAction: New list after map: $newList")
                newList
            }
        }
    }


    // Renamed and modified to be triggered by the adapter after a move
    fun onActionListReordered(reorderedActions: List<Action>) {
        Timber.d("Lista de acciones reordenada en el ViewModel: $reorderedActions")
        _actions.value = reorderedActions // Update the ViewModel's list
        // The saving of the new order happens when saveRoutine is called
    }


    fun removeAction(action: Action) {
        Timber.d("Eliminando acción: $action")
        viewModelScope.launch {
            // Consider if you need to delete from the database immediately or only on save
            // For now, we'll remove from the ViewModel's list
            _actions.update { it.filterNot { it.uuid == action.uuid } }
            Timber.d("Acción eliminada de la lista del ViewModel: ${_actions.value}")
            // If you need to delete from DB immediately: repository.deleteAction(action)
        }
    }

    fun addAction(action: Action) {
        Timber.d("addAction called with: $action")
        Timber.d("addAction: Current actions before adding: ${_actions.value}")
        val currentActions = _actions.value.toMutableList()
        // Generar UUID solo si está vacío
        val actionToAdd = if (action.uuid.isEmpty()) {
            action.copy(uuid = UUID.randomUUID().toString())
        } else {
            action // Usar el UUID existente si ya lo tiene
        }
        currentActions.add(actionToAdd) // Usar actionToAdd aquí
        _actions.value = currentActions.toList()
        Timber.d("Nueva acción con UUID añadida a la lista del ViewModel: ${_actions.value}")
        // Opcional: Devuelve la acción añadida con el UUID para que el Fragment la use directamente
        // return actionToAdd
    }

    fun saveRoutine(name: String) { // Ya no recibe triggers ni actions
        Timber.d("Guardando rutina con nombre: $name")
        viewModelScope.launch {
            val currentRoutine = _currentRoutine.value
            if (currentRoutine == null) {
                _saveResult.emit(Resource.Error("No se puede guardar la rutina: _currentRoutine es nulo"))
                Timber.e("Error al guardar rutina: _currentRoutine es nulo")
                return@launch
            }

            // Validaciones de triggers (usando el estado del ViewModel)
            for (trigger in currentRoutine.triggers) { // Usa triggers del ViewModel
                if (trigger.triggerType == "TIME") {
                    val frequency = trigger.data.data["frequency"] as? String
                    val daysOfWeek = trigger.data.data["daysOfWeek"] as? List<Int>
                    val dayOfMonth = trigger.data.data["dayOfMonth"] as? Int

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
                val routineToSave = currentRoutine.copy(
                    name = name,
                    // triggers y actions ya están en los StateFlows del ViewModel
                    triggers = currentRoutine.triggers, // Usa triggers del ViewModel
                    actions = _actions.value // Usa actions del ViewModel
                )

                val resultId = if (isNewRoutine) {
                    repository.insertRoutine(routineToSave) // Asume que insertRoutine devuelve el ID
                } else {
                    repository.updateRoutine(routineToSave) // Asume que updateRoutine actualiza y no devuelve nada o el ID
                    routineToSave.id // Si es update, usa el ID existente
                }

                // Emite un resultado de éxito con el ID guardado/actualizado
                _saveResult.emit(Resource.Success(resultId)) // Ajusta según lo que devuelva insert/update

                // Actualiza el StateFlow _currentRoutine con el ID si era una nueva rutina
                if (isNewRoutine) {
                    _currentRoutine.value = routineToSave.copy(id = resultId)
                    isNewRoutine = false // Ya no es una rutina nueva después de guardar
                }


                Timber.d("Rutina guardada con éxito, ID: $resultId")

            } catch (e: Exception) {
                Timber.e("Error inesperado al guardar rutina: ${e.message}")
                _saveResult.emit(Resource.Error(e.message ?: "Error desconocido"))
            }
        }
    }

    fun addTrigger(trigger: Trigger) {
        Timber.d("Agregando trigger: $trigger")
        _currentRoutine.update { currentRoutine ->
            currentRoutine?.copy(triggers = currentRoutine.triggers + trigger)
        }
        Timber.d("Trigger agregado, lista de triggers actualizada: ${_currentRoutine.value?.triggers}")
    }
}