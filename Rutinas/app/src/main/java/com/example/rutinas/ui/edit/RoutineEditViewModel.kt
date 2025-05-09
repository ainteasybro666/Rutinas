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

    private val _actions = MutableStateFlow<MutableList<Action>>(mutableListOf()) // Usar MutableList
    val actions: StateFlow<List<Action>> = _actions.asStateFlow()

    private val _triggers = MutableStateFlow<MutableList<Trigger>>(mutableListOf()) // Usar MutableList
    val triggers: StateFlow<List<Trigger>> = _triggers.asStateFlow()

    private val _saveResult = MutableSharedFlow<Resource<Long>>()
    val saveResult: SharedFlow<Resource<Long>> = _saveResult.asSharedFlow()

    private var isNewRoutine: Boolean = true // Flag to track if it's a new routine

    // Load routine based on UUID, or initialize for a new routine
    fun loadRoutine(uuid: String?) { // Acepta String?
        if (uuid.isNullOrEmpty()) { // Usar isNullOrEmpty para manejar null y cadena vacía
            // Creating a new routine
            isNewRoutine = true
            val newUuid = UUID.randomUUID().toString()
            _currentRoutine.value = Routine(id = 0, uuid = newUuid, name = "", triggers = emptyList(), actions = emptyList())
            _actions.value = mutableListOf() // Initial state for a new routine
            _triggers.value = mutableListOf() // Initial state for triggers
            Timber.d("ViewModel: Initializing ViewModel for new routine with UUID: $newUuid") // Log initialization
        } else {
            // Editing existing routine
            isNewRoutine = false
            Timber.d("ViewModel: Loading routine with UUID: $uuid") // Log loading existing routine
            viewModelScope.launch {
                val routine = repository.getRoutineByUuid(uuid)
                Timber.d("ViewModel: Repository returned routine: $routine") // Log repository result
                if (routine != null) {
                    _currentRoutine.value = routine
                    _triggers.value = routine.triggers.toMutableList() // Load existing triggers as MutableList
                    _actions.value = routine.actions.toMutableList() // Load existing actions as MutableList
                    Timber.d("Rutina cargada: $routine")
                } else {
                    Timber.e("Routine with UUID $uuid not found.")
                    // Manejar el caso donde la rutina no se encuentra.
                    // Podrías emitir un error, navegar de regreso, etc.
                    // Por ahora, simplemente registramos el error y dejamos los StateFlows vacíos.
                    _currentRoutine.value = null // Indicar que no se cargó ninguna rutina
                    _triggers.value = mutableListOf()
                    _actions.value = mutableListOf()
                }
            }
        }
    }

    // Modificado para usar MutableList y emitir la lista actualizada
    fun updateAction(updatedAction: Action) {
        Timber.d("ViewModel: updateAction called with: $updatedAction") // Log update call
        val currentActions = _actions.value // Obtener la lista mutable actual
        Timber.d("ViewModel: updateAction: Current actions before update: ${currentActions.map { it.uuid to it.executionOrder }}") // Log current state with UUIDs and order
        val index = currentActions.indexOfFirst { it.uuid == updatedAction.uuid }
        if (index != -1) {
            currentActions[index] = updatedAction
            _actions.value = currentActions.toMutableList() // Emitir una nueva instancia
            Timber.d("updateAction: Action with UUID ${updatedAction.uuid} updated. New list: ${_actions.value}")
        } else {
            Timber.w("updateAction: Action with UUID ${updatedAction.uuid} not found in list.")
        }
    }


    // Renamed and modified to be triggered by the adapter after a move
    // Ya usa MutableList, solo necesitamos emitir la lista reordenada
    fun onActionListReordered(reorderedActions: List<Action>) {
        Timber.d("ViewModel: Lista de acciones reordenada en el ViewModel: ${reorderedActions.map { it.uuid to it.executionOrder }}") // Log reordered list with UUIDs and order
        // Asegurarse de que reorderedActions es un MutableList si necesitas mutarlo después
        _actions.value = reorderedActions.toMutableList() // Update the ViewModel's list and emit
        Timber.d("ViewModel actions updated after reorder: ${_actions.value}")
        // The saving of the new order happens when saveRoutine is called
    }


    // Modificado para usar MutableList y emitir la lista actualizada
    fun removeAction(action: Action) {
        Timber.d("ViewModel: Eliminando acción: $action") // Log removal call
        val currentActions = _actions.value
        if (currentActions.remove(action)) { // Remove returns true if successful
            // Re-index executionOrder after removing
            currentActions.forEachIndexed { index, act -> act.executionOrder = index }
            _actions.value = currentActions.toMutableList() // Emit a new instance
            Timber.d("Acción eliminada de la lista del ViewModel: ${_actions.value}")
        } else {
            Timber.w("Attempted to remove an action that was not found: $action")
        }
        // Consider if you need to delete from the database immediately or only on save
        // For now, we'll remove from the ViewModel's list
        // If you need to delete from DB immediately: viewModelScope.launch { repository.deleteAction(action) }
    }

    // Modificado para usar MutableList, generar UUID y devolver la acción añadida
    fun addAction(action: Action): Action? { // Devuelve la acción añadida (con UUID)
        Timber.d("ViewModel: addAction called with: $action") // Log add call
        val currentActions = _actions.value
        // Generar UUID solo si está vacío
        val actionToAdd = if (action.uuid.isEmpty()) {
            action.copy(uuid = UUID.randomUUID().toString())
        } else {
            action // Usar el UUID existente si ya lo tiene
        }
        currentActions.add(actionToAdd) // Usar actionToAdd aquí
        _actions.value = currentActions.toMutableList() // Emitir una nueva instancia
        Timber.d("Nueva acción con UUID añadida a la lista del ViewModel: ${_actions.value}")
        return actionToAdd // Devuelve la acción añadida con el UUID
    }

    fun saveRoutine(name: String) { // Ya no recibe triggers ni actions
        Timber.d("ViewModel: Guardando rutina con nombre: $name") // Log save call
        viewModelScope.launch {
            _saveResult.emit(Resource.Loading) // Emitir estado de carga

            val currentRoutine = _currentRoutine.value
            if (currentRoutine == null) {
                _saveResult.emit(Resource.Error("No se puede guardar la rutina: _currentRoutine es nulo"))
                Timber.e("Error al guardar rutina: _currentRoutine es nulo")
                return@launch
            }

            Timber.d("ViewModel: Validando triggers antes de guardar: ${_triggers.value.size} triggers") // Log validation start

            // Validaciones de triggers (usando el StateFlow del ViewModel)
            for (trigger in _triggers.value) { // Usa la lista de triggers del StateFlow
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
                // Puedes añadir más validaciones para otros tipos de triggers aquí
            }

            try {
                // Crear una nueva Routine con los datos actualizados
                val routineToSave = currentRoutine.copy(
                    name = name,
                    // triggers y actions ya están en los StateFlows del ViewModel
                    triggers = _triggers.value.toList(), // Usa la lista de triggers del StateFlow
                    actions = _actions.value.toList() // Usa actions del ViewModel
                )

                val resultId = if (isNewRoutine) {
                    repository.insertRoutine(routineToSave) // Asume que insertRoutine devuelve el ID (Long)
                } else {
                    Timber.d("ViewModel: Actualizando rutina existente con ID: ${routineToSave.id}") // Log update
                    repository.updateRoutine(routineToSave) // Asume que updateRoutine actualiza y no devuelve nada o el ID
                    routineToSave.id // Si es update, usa el ID existente
                }

                // Emite un resultado de éxito con el ID guardado/actualizado
                _saveResult.emit(Resource.Success(resultId)) // Ajusta según lo que devuelva insert/update

                // Actualiza el StateFlow _currentRoutine con el ID si era una nueva rutina
                // Esto es importante para que las acciones/triggers añadidos después de guardar tengan el routineId correcto
                if (isNewRoutine) {
                    Timber.d("Updating _currentRoutine with new ID: $resultId")
                    // Asegurar que creamos una nueva instancia de Routine con el ID correcto
                    _currentRoutine.value = routineToSave.copy(id = resultId, uuid = routineToSave.uuid) // Asegurar que el UUID también se mantiene

                    // Also update the routineId for triggers and actions in the ViewModel's StateFlows
                    // This is crucial for adding new triggers/actions after the initial save
                    _triggers.update { currentList ->
                        currentList.map { it.copy(routineId = resultId) }.toMutableList()
                    }
                    _actions.update { currentList ->
                        currentList.map { it.copy(routineId = resultId) }.toMutableList()
                    }

                    isNewRoutine = false // Ya no es una rutina nueva después de guardar
                }


                Timber.d("Rutina guardada con éxito, ID: $resultId")

            } catch (e: Exception) {
                Timber.e("Error inesperado al guardar rutina: ${e.message}", e) // Loguear la excepción
                _saveResult.emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
            }
        }
    }


    // Modificado para usar MutableList, generar UUID y devolver el trigger añadido
    fun addTrigger(trigger: Trigger): Trigger? { // Devuelve el trigger añadido (con UUID)
        Timber.d("ViewModel: addTrigger called with: $trigger") // Log add call
        val currentTriggers = _triggers.value
        val triggerToAdd = if (trigger.uuid.isEmpty()) {
            trigger.copy(uuid = UUID.randomUUID().toString())
        } else {
            trigger
        }
        currentTriggers.add(triggerToAdd)
        _triggers.value = currentTriggers.toMutableList() // Emitir una nueva instancia
        Timber.d("Trigger añadido a la lista del ViewModel: ${_triggers.value}")
        return triggerToAdd // Devuelve el trigger añadido con el UUID
    }

    // Modificado para usar MutableList y emitir la lista actualizada
    fun removeTrigger(trigger: Trigger) {
        Timber.d("ViewModel: Eliminando trigger: $trigger") // Log removal call
        val currentTriggers = _triggers.value
        if(currentTriggers.remove(trigger)) { // Remove returns true if successful
            _triggers.value = currentTriggers.toMutableList() // Emitir una nueva instancia
            Timber.d("Trigger eliminado de la lista del ViewModel: ${_triggers.value}")
        } else {
            Timber.w("Attempted to remove a trigger that was not found: $trigger")
        }
    }

    // Méto-do para actualizar un trigger existente (si necesitas esta funcionalidad)
    fun updateTrigger(updatedTrigger: Trigger) {
        Timber.d("ViewModel: updateTrigger called with: $updatedTrigger") // Log update call
        val currentTriggers = _triggers.value
        val index = currentTriggers.indexOfFirst { it.uuid == updatedTrigger.uuid }
        if (index != -1) {
            currentTriggers[index] = updatedTrigger
            _triggers.value = currentTriggers.toMutableList() // Emitir una nueva instancia
            Timber.d("updateTrigger: Trigger with UUID ${updatedTrigger.uuid} updated. New list: ${_triggers.value}")
        } else {
            Timber.w("updateTrigger: Trigger with UUID ${updatedTrigger.uuid} not found in list.")
        }
    }
}
