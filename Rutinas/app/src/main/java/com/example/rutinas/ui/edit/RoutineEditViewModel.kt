package com.example.rutinas.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.FrequencyType
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import com.example.rutinas.ui.edit.dialogs.TriggerTypeDialog
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
import timber.log.Timber.Forest.e
import java.time.LocalDateTime
import java.util.Collections.frequency
import java.util.UUID // Import UUID
import javax.inject.Inject


@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    private val repository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _currentRoutine = MutableStateFlow<Routine?>(null)
    val currentRoutine: StateFlow<Routine?> = _currentRoutine.asStateFlow()

    private val _actions = MutableStateFlow<List<Action>>(emptyList())
    val actions: StateFlow<List<Action>> = _actions.asStateFlow()

    private val _triggers = MutableStateFlow<List<Trigger>>(emptyList())
    val triggers: StateFlow<List<Trigger>> = _triggers.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
    }

    private val _saveResult = MutableSharedFlow<Resource<Long>>()
    val saveResult: SharedFlow<Resource<Long>> = _saveResult.asSharedFlow()

    private var isNewRoutine: Boolean = true

    fun loadRoutine(uuid: String?) {
        Timber.d("ViewModel: loadRoutine called with UUID: $uuid") // <<< Log de inicio de carga
        if (uuid.isNullOrEmpty()) {
            isNewRoutine = true
            val newUuid = UUID.randomUUID().toString()
            _currentRoutine.value = Routine(id = 0, uuid = newUuid, name = "", triggers = emptyList(), actions = emptyList())
            _actions.value = emptyList()
            _triggers.value = emptyList()
            Timber.d("ViewModel: Initializing ViewModel for new routine with UUID: $newUuid")
        } else {
            isNewRoutine = false
            Timber.d("ViewModel: Loading existing routine with UUID: $uuid")
            viewModelScope.launch {
                val routine = repository.getRoutineByUuid(uuid)
                Timber.d("ViewModel: Repository returned routine: $routine") // <<< Log del resultado del repositorio
                if (routine != null) {
                    _currentRoutine.value = routine
                    _triggers.value = routine.triggers.toList()
                    _actions.value = routine.actions.toList()

                    Timber.d("ViewModel: Rutina cargada y asignada a StateFlows.")
                    // <<< Inspeccionar el DataWrapper del trigger de tiempo justo después de cargar
                    val loadedTimeTrigger = routine.triggers.find { it.triggerType == TriggerTypeDialog.TriggerType.TIME }
                    if (loadedTimeTrigger != null) {
                        Timber.d("ViewModel: Trigger de tiempo encontrado en la rutina CARGADA (ANTES de StateFlow). Inspecting data:")
                        loadedTimeTrigger.data.data.forEach { (key, value) ->
                            Timber.d("  LOADED Routine - Key: $key, Value: $value, Type: ${value?.javaClass?.name}")
                        }
                    } else {
                        Timber.d("ViewModel: No se encontró trigger de tiempo en la rutina CARGADA.")
                    }

                    // <<< Inspeccionar el DataWrapper del trigger de tiempo DESPUÉS de asignarlo al StateFlow
                    val stateFlowTimeTrigger = _triggers.value.find { it.triggerType == TriggerTypeDialog.TriggerType.TIME }
                    if (stateFlowTimeTrigger != null) {
                        Timber.d("ViewModel: Trigger de tiempo en el _triggers StateFlow (DESPUÉS de asignación). Inspecting data:")
                        stateFlowTimeTrigger.data.data.forEach { (key, value) ->
                            Timber.d("  StateFlow AFTER Assignment - Key: $key, Value: $value, Type: ${value?.javaClass?.name}")
                        }
                    } else {
                        Timber.d("ViewModel: No se encontró trigger de tiempo en el _triggers StateFlow (DESPUÉS de asignación).")
                    }

                } else {
                    Timber.e("Routine with UUID $uuid not found.")
                    _currentRoutine.value = null
                    _triggers.value = emptyList()
                    _actions.value = emptyList()
                    Timber.d("ViewModel: StateFlows vaciados debido a rutina no encontrada.")
                }
            }
        }
    }

    fun updateAction(updatedAction: Action) {
        Timber.d("ViewModel: updateAction called with UUID: ${updatedAction.uuid}")
        _actions.update { currentActions ->
            val index = currentActions.indexOfFirst { it.uuid == updatedAction.uuid }
            if (index != -1) {
                currentActions.toMutableList().apply { this[index] = updatedAction }.toList()
            } else {
                Timber.w("updateAction: Action with UUID ${updatedAction.uuid} not found.")
                currentActions
            }
        }
        Timber.d("updateAction: Action UUID ${updatedAction.uuid} updated. New list size: ${_actions.value.size}")
    }

    fun onActionListReordered(reorderedActions: List<Action>) {
        Timber.d("ViewModel: onActionListReordered called. Reordered actions size: ${reorderedActions.size}")
        // Log UUID and order of reordered actions
        reorderedActions.forEachIndexed { index, action ->
            Timber.d("  Reordered Action ${index}: UUID = ${action.uuid}, Order = ${action.executionOrder}")
        }
        _actions.value = reorderedActions.toList()
        Timber.d("ViewModel actions StateFlow updated after reorder. New size: ${_actions.value.size}")
    }

    fun removeAction(action: Action) {
        Timber.d("ViewModel: removeAction called with UUID: ${action.uuid}")
        _actions.update { currentActions ->
            val updatedList = currentActions.filter { it.uuid != action.uuid }
            updatedList.mapIndexed { index, act -> act.copy(executionOrder = index) }
        }
        Timber.d("Action UUID ${action.uuid} removed. New actions list size: ${_actions.value.size}")
    }

    fun addAction(action: Action): Action {
        Timber.d("ViewModel: addAction called with type: ${action.actionType}")
        val actionToAdd = if (action.uuid.isEmpty() || action.uuid == "0") {
            action.copy(uuid = UUID.randomUUID().toString())
        } else {
            action
        }
        _actions.update { currentActions ->
            currentActions + actionToAdd
        }
        Timber.d("New action with UUID ${actionToAdd.uuid} added. Total actions size: ${_actions.value.size}")
        return actionToAdd
    }

    fun saveRoutine(name: String) {
        Timber.d("ViewModel: Guardando rutina con nombre: $name")
        viewModelScope.launch {
            _saveResult.emit(Resource.Loading)

            val currentRoutine = _currentRoutine.value
            if (currentRoutine == null) {
                _saveResult.emit(Resource.Error("No se puede guardar la rutina: _currentRoutine es nulo"))
                Timber.e("Error al guardar rutina: _currentRoutine es nulo")
                return@launch
            }

            Timber.d("ViewModel: Iniciando validación de triggers antes de guardar. Cantidad: ${_triggers.value.size}")

            // Validaciones de triggers
            for (trigger in _triggers.value) {
                Timber.d("ViewModel: Validando trigger con UUID: ${trigger.uuid}, Tipo: ${trigger.triggerType}")
                Timber.d("ViewModel: Trigger data para validación: ${trigger.data.data}") // <<< Log de datos antes de validación

                if (trigger.triggerType == TriggerTypeDialog.TriggerType.TIME) {
                    Timber.d("ViewModel: Validando trigger de tiempo.")
                    // Obtener los datos de forma segura para validación
                    val frequency = trigger.getFrequencyType("frequency") // <<< Usar el getter seguro
                    val daysOfWeek = trigger.getDaysOfWeek("daysOfWeek") // <<< Usar el getter seguro
                    val dayOfMonth = trigger.getInt("dayOfMonth") // <<< Usar el getter seguro

                    // <<< Logs específicos para los valores de trigger de tiempo antes de la validación
                    Timber.d("ViewModel: Validating TIME trigger - frequency: $frequency (${frequency?.javaClass?.name}), daysOfWeek: $daysOfWeek (${daysOfWeek?.javaClass?.name}), dayOfMonth: $dayOfMonth (${dayOfMonth?.javaClass?.name})")


                    if (frequency == FrequencyType.WEEKLY) {
                        if (daysOfWeek.isNullOrEmpty()) {
                            Timber.d("ViewModel: Validación fallida - Trigger semanal sin días seleccionados.")
                            _uiEvent.emit(UiEvent.ShowMessage("Debes seleccionar al menos un día de la semana para un trigger semanal."))
                            _saveResult.emit(Resource.Error("Debes seleccionar al menos un día de la semana para un trigger semanal."))
                            return@launch
                        }
                        Timber.d("ViewModel: Validación exitosa - Trigger semanal.")
                    }

                    if (frequency == FrequencyType.MONTHLY) {
                        if (dayOfMonth == null || dayOfMonth !in 1..31) {
                            Timber.d("ViewModel: Validación fallida - Trigger mensual con día inválido.")
                            _uiEvent.emit(UiEvent.ShowMessage("El día del mes debe estar entre 1 y 31 para un trigger mensual."))
                            _saveResult.emit(Resource.Error("El día del mes debe estar entre 1 y 31 para un trigger mensual."))
                            return@launch
                        }
                        Timber.d("ViewModel: Validación exitosa - Trigger mensual.")
                    }

                    if (frequency == null || (frequency != FrequencyType.ONCE && frequency != FrequencyType.WEEKLY && frequency != FrequencyType.MONTHLY)) {
                        Timber.d("ViewModel: Validación fallida - Frecuencia de trigger de tiempo no válida o nula: $frequency")
                        _uiEvent.emit(UiEvent.ShowMessage("Frecuencia del trigger de tiempo no válida."))
                        _saveResult.emit(Resource.Error("Frecuencia del trigger de tiempo no válida."))
                        return@launch
                    }
                    Timber.d("ViewModel: Validación exitosa - Frecuencia de trigger de tiempo.")
                }

                if (trigger.triggerType == TriggerTypeDialog.TriggerType.CALENDAR) {
                    Timber.d("ViewModel: Validando trigger de calendario.")
                    val calendarTime = trigger.getLocalDateTime("calendarTime")
                    if (calendarTime == null) {
                        Timber.d("ViewModel: Validación fallida - Trigger de calendario sin fecha/hora.")
                        _uiEvent.emit(UiEvent.ShowMessage("La fecha y hora del trigger de calendario no pueden estar vacías."))
                        _saveResult.emit(Resource.Error("La fecha y hora del trigger de calendario no pueden estar vacías."))
                        return@launch
                    }
                    Timber.d("ViewModel: Validación exitosa - Trigger de calendario.")
                }
                // Add validation logs for LOCATION trigger if needed
//                 if (trigger.triggerType == TriggerTypeDialog.TriggerType.LOCATION) {
//                      Timber.d("ViewModel: Validando trigger de ubicación.")
//                      // ... validación y logs para ubicación ...
//                 }
            }
            Timber.d("ViewModel: Todas las validaciones de triggers pasaron.")

            try {
                Timber.d("ViewModel: Procediendo a guardar rutina en BD.")
                val routineToSave = currentRoutine.copy(
                    name = name,
                    triggers = _triggers.value.toList(),
                    actions = _actions.value.toList()
                )
                Timber.d("ViewModel: Rutina a guardar: ID=${routineToSave.id}, UUID=${routineToSave.uuid}, Nombre=${routineToSave.name}, Triggers size=${routineToSave.triggers.size}, Actions size=${routineToSave.actions.size}")


                val resultId = if (isNewRoutine) {
                    val insertedId = repository.insertRoutine(routineToSave)
                    Timber.d("ViewModel: Rutina INSERTADA con ID: $insertedId")
                    insertedId
                } else {
                    Timber.d("ViewModel: Rutina ACTUALIZANDO existente con ID: ${routineToSave.id}")
                    repository.updateRoutine(routineToSave)
                    routineToSave.id
                }

                val savedRoutine = repository.getRoutineById(resultId)

                if (savedRoutine != null) {
                    Timber.d("ViewModel: Rutina guardada obtenida de nuevo (ID: $resultId) para programar alarmas.")
                    // <<< Inspeccionar el DataWrapper del trigger de tiempo en la rutina recién guardada (obtenida de nuevo)
                    val savedTimeTrigger = savedRoutine.triggers.find { it.triggerType == TriggerTypeDialog.TriggerType.TIME }
                    if (savedTimeTrigger != null) {
                        Timber.d("ViewModel: Trigger de tiempo encontrado en la rutina GUARDADA (obtenida de nuevo). Inspecting data BEFORE scheduling:")
                        savedTimeTrigger.data.data.forEach { (key, value) ->
                            Timber.d("  SAVED Routine - Key: $key, Value: $value, Type: ${value?.javaClass?.name}")
                        }
                    } else {
                        Timber.d("ViewModel: No se encontró trigger de tiempo en la rutina GUARDADA (obtenida de nuevo).")
                    }

                    alarmScheduler.scheduleRoutineAlarms(savedRoutine)
                    Timber.d("ViewModel: alarmScheduler.scheduleRoutineAlarms() llamado con rutina ID: ${savedRoutine.id}")
                } else {
                    Timber.e("ViewModel: No se pudo obtener la rutina guardada (ID: $resultId) para programar alarmas.")
                }

                _saveResult.emit(Resource.Success(resultId))

                if (isNewRoutine) {
                    Timber.d("ViewModel: Es nueva rutina, actualizando _currentRoutine con ID: $resultId")
                    _currentRoutine.value = routineToSave.copy(id = resultId, uuid = routineToSave.uuid)
                    _triggers.update { currentList -> currentList.map { it.copy(routineId = resultId) }.toList() }
                    _actions.update { currentList -> currentList.map { it.copy(routineId = resultId) }.toList() }
                    isNewRoutine = false
                    Timber.d("ViewModel: _currentRoutine, _triggers, _actions actualizados para nueva rutina.")
                } else {
                    Timber.d("ViewModel: Rutina existente actualizada. StateFlows ya deberían tener los datos correctos.")
                }


                Timber.d("ViewModel: Rutina guardada con éxito, ID: $resultId")

            } catch (e: Exception) {
                Timber.e("ViewModel: Error inesperado al guardar rutina: ${e.message}", e)
                viewModelScope.launch {
                    _uiEvent.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Error desconocido al guardar rutina"))
                }
                _saveResult.emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
            }
        }
    }

    fun addTrigger(trigger: Trigger): Resource<Trigger> {
        Timber.d("ViewModel: addTrigger called with type: ${trigger.triggerType}, initial UUID: ${trigger.uuid}")

        val existingTrigger = _triggers.value.find { it.triggerType == trigger.triggerType }
        if (existingTrigger != null) {
            val errorMessage = "Ya existe un trigger de tipo ${trigger.triggerType} para esta rutina. Solo se permite uno de cada tipo."
            Timber.w("ViewModel: Intento de añadir trigger duplicado de tipo: ${trigger.triggerType}")
            // No emitir evento de UI aquí, la pantalla que llama a addTrigger puede manejar el Resource.Error
            return Resource.Error(errorMessage)
        }

        val triggerToAdd = if (trigger.uuid.isEmpty() || trigger.uuid == "0") {
            trigger.copy(uuid = UUID.randomUUID().toString())
        } else {
            trigger // Usar UUID existente si ya lo tiene (ej: viene de un trigger editado)
        }

        _triggers.update { currentTriggers ->
            currentTriggers + triggerToAdd
        }
        Timber.d("ViewModel: Trigger añadido exitosamente. UUID asignado: ${triggerToAdd.uuid}. Lista total size: ${_triggers.value.size}")
        // Log el data del trigger recién añadido
        Timber.d("ViewModel: Data del trigger recién añadido (UUID: ${triggerToAdd.uuid}): ${triggerToAdd.data.data}")

        return Resource.Success(triggerToAdd)
    }

    fun saveTrigger(trigger: Trigger) {
        Timber.d("ViewModel: saveTrigger called with trigger UUID: ${trigger.uuid}")

        val existingTriggerIndex = _triggers.value.indexOfFirst { it.uuid == trigger.uuid }

        if (existingTriggerIndex != -1) {
            Timber.d("ViewModel: Trigger with UUID ${trigger.uuid} found in list. Calling updateTrigger.")
            updateTrigger(trigger)
        } else {
            Timber.d("ViewModel: Trigger with UUID ${trigger.uuid} not found in list. Calling addTrigger.")
            val addResult = addTrigger(trigger)
            when(addResult) {
                is Resource.Success -> Timber.d("ViewModel: addTrigger called from saveTrigger - SUCCESS. New UUID: ${addResult.data.uuid}")
                is Resource.Error -> {
                    Timber.e("ViewModel: addTrigger called from saveTrigger - FAILED: ${addResult.message}")
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.ShowMessage(addResult.message ?: "Error desconocido al añadir trigger"))
                    }
                }
                else -> { /* Loading */ }
            }
        }
        Timber.d("ViewModel: saveTrigger finished.")
    }


    fun removeTrigger(trigger: Trigger) {
        Timber.d("ViewModel: removeTrigger called with UUID: ${trigger.uuid}")
        _triggers.update { currentTriggers ->
            currentTriggers.filter { it.uuid != trigger.uuid }
        }
        Timber.d("Trigger UUID ${trigger.uuid} removed. New triggers list size: ${_triggers.value.size}")
    }

    fun updateTrigger(updatedTrigger: Trigger) {
        Timber.d("ViewModel: updateTrigger called with UUID: ${updatedTrigger.uuid}")
        _triggers.update { currentTriggers ->
            val index = currentTriggers.indexOfFirst { it.uuid == updatedTrigger.uuid }
            if (index != -1) {
                val updatedList = currentTriggers.toMutableList().apply { this[index] = updatedTrigger }.toList()
                Timber.d("ViewModel: Trigger with UUID ${updatedTrigger.uuid} updated in list.")
                // Log el data del trigger actualizado
                Timber.d("ViewModel: Data del trigger actualizado (UUID: ${updatedTrigger.uuid}): ${updatedTrigger.data.data}")
                updatedList
            } else {
                Timber.w("updateTrigger: Trigger with UUID ${updatedTrigger.uuid} not found in list.")
                currentTriggers
            }
        }
        Timber.d("ViewModel: updateTrigger finished for UUID ${updatedTrigger.uuid}. New list size: ${_triggers.value.size}")
    }
}