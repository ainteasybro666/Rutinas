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


@HiltViewModel // Anotación si usas Hilt
class RoutineEditViewModel @Inject constructor(
    private val repository: RoutineRepository
) : ViewModel() {

    private val _currentRoutine = MutableStateFlow<Routine?>(null)
    val currentRoutine: StateFlow<Routine?> = _currentRoutine.asStateFlow()

    // Cambiar a StateFlow<List<Action>> (lista inmutable)
    private val _actions = MutableStateFlow<List<Action>>(emptyList())
    val actions: StateFlow<List<Action>> = _actions.asStateFlow()

    // Cambiar a StateFlow<List<Trigger>> (lista inmutable)
    private val _triggers = MutableStateFlow<List<Trigger>>(emptyList())
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
            _actions.value = emptyList() // Initial state for a new routine
            _triggers.value = emptyList() // Initial state for triggers
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
                    _triggers.value = routine.triggers.toList() // Load existing triggers as immutable list
                    _actions.value = routine.actions.toList() // Load existing actions as immutable list
                    Timber.d("Rutina cargada: $routine")
                } else {
                    Timber.e("Routine with UUID $uuid not found.")
                    // Manejar el caso donde la rutina no se encuentra.
                    // Podrías emitir un error, navegar de regreso, etc.
                    // Por ahora, simplemente registramos el error y dejamos los StateFlows vacíos.
                    _currentRoutine.value = null // Indicar que no se cargó ninguna rutina
                    _triggers.value = emptyList()
                    _actions.value = emptyList()
                }
            }
        }
    }

    // Modificado para usar listas inmutables y crear una nueva lista
    fun updateAction(updatedAction: Action) {
        Timber.d("ViewModel: updateAction called with: $updatedAction") // Log update call
        _actions.update { currentActions -> // Usar la función update
            val index = currentActions.indexOfFirst { it.uuid == updatedAction.uuid }
            if (index != -1) {
                // Crear una nueva lista con el elemento actualizado
                currentActions.toMutableList().apply { this[index] = updatedAction }.toList()
            } else {
                Timber.w("updateAction: Action with UUID ${updatedAction.uuid} not found in list.")
                currentActions // Devolver la lista actual si no se encontró
            }
        }
        Timber.d("updateAction: Action with UUID ${updatedAction.uuid} updated. New list size: ${_actions.value.size}")
    }

    // Renamed and modified to be triggered by the adapter after a move
    // Ya usa MutableList, solo necesitamos emitir la lista reordenada
    fun onActionListReordered(reorderedActions: List<Action>) {
        Timber.d("ViewModel: Lista de acciones reordenada en el ViewModel: ${reorderedActions.map { it.uuid to it.executionOrder }}") // Log reordered list with UUIDs and order
        // Emitir la lista reordenada como un nuevo valor (lista inmutable)
        _actions.value = reorderedActions.toList()
        Timber.d("ViewModel actions updated after reorder: ${_actions.value.size}")
        // The saving of the new order happens when saveRoutine is called
    }

    // Modificado para usar listas inmutables y crear una nueva lista
    fun removeAction(action: Action) {
        Timber.d("ViewModel: Eliminando acción: $action") // Log removal call
        _actions.update { currentActions -> // Usar la función update
            val updatedList = currentActions.filter { it.uuid != action.uuid }
            // Re-index executionOrder after removing on the new list
            updatedList.mapIndexed { index, act -> act.copy(executionOrder = index) }
        }
        Timber.d("Acción eliminada de la lista del ViewModel. Nueva lista size: ${_actions.value.size}")
    }

    // Modificado para usar listas inmutables, generar UUID y devolver la acción añadida
    fun addAction(action: Action): Action { // Devuelve la acción añadida (con UUID) - asumimos que siempre se añade
        Timber.d("ViewModel: addAction called with: ${action.actionType}") // Log add call
        val actionToAdd = if (action.uuid.isEmpty()) {
            action.copy(uuid = UUID.randomUUID().toString())
        } else {
            action // Usar el UUID existente si ya lo tiene
        }
        // Crear una nueva lista con la acción añadida y emitir
        _actions.update { currentActions ->
            currentActions + actionToAdd
        }
        Timber.d("Nueva acción con UUID añadida a la lista del ViewModel: ${actionToAdd.uuid}. Lista total size: ${_actions.value.size}")
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
                    // Obtener los datos de forma segura
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
                    triggers = _triggers.value.toList(), // Usa la lista de triggers del StateFlow (ya es inmutable, pero toList crea una nueva instancia)
                    actions = _actions.value.toList() // Usa actions del ViewModel (ya es inmutable, pero toList crea una nueva instancia)
                )

                val resultId = if (isNewRoutine) {
                    val insertedId = repository.insertRoutine(routineToSave) // Asume que insertRoutine devuelve el ID (Long)
                    Timber.d("ViewModel: Rutina insertada con ID: $insertedId")
                    insertedId
                } else {
                    Timber.d("ViewModel: Actualizando rutina existente con ID: ${routineToSave.id}") // Log update
                    repository.updateRoutine(routineToSave) // Asume que updateRoutine actualiza
                    routineToSave.id // Si es update, usa el ID existente
                }

                // Emite un resultado de éxito con el ID guardado/actualizado
                _saveResult.emit(Resource.Success(resultId)) // Ajusta según lo que devuelva insert/update

                // Actualiza el StateFlow _currentRoutine con el ID si era una nueva rutina
                // Esto es importante para que las acciones/triggers añadidos después de guardar tengan el routineId correcto
                if (isNewRoutine) {
                    Timber.d("Updating _currentRoutine with new ID: $resultId")
                    // Asegurar que creamos una nueva instancia de Routine con el ID correcto y manteniendo el UUID
                    _currentRoutine.value = routineToSave.copy(id = resultId, uuid = routineToSave.uuid)

                    // Also update the routineId for triggers and actions in the ViewModel's StateFlows
                    // This is crucial for adding new triggers/actions after the initial save
                    _triggers.update { currentList ->
                        currentList.map { it.copy(routineId = resultId) }.toList() // Usar toList() para emitir nueva instancia
                    }
                    _actions.update { currentList ->
                        currentList.map { it.copy(routineId = resultId) }.toList() // Usar toList() para emitir nueva instancia
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

    // Modificado para usar listas inmutables, generar UUID y devolver el trigger añadido
    fun addTrigger(trigger: Trigger): Trigger { // Devuelve el trigger añadido (con UUID) - asumimos que siempre se añade
        Timber.d("ViewModel: addTrigger called with: ${trigger.triggerType}") // Log add call
        val triggerToAdd = if (trigger.uuid.isEmpty()) {
            trigger.copy(uuid = UUID.randomUUID().toString())
        } else {
            trigger
        }
        // Crear una nueva lista con el trigger añadido y emitir
        _triggers.update { currentTriggers ->
            currentTriggers + triggerToAdd
        }
        Timber.d("Trigger añadido a la lista del ViewModel: ${triggerToAdd.uuid}. Lista total size: ${_triggers.value.size}")
        return triggerToAdd // Devuelve el trigger añadido con el UUID
    }

    // Modificado para usar listas inmutables y crear una nueva lista
    fun removeTrigger(trigger: Trigger) {
        Timber.d("ViewModel: Eliminando trigger: $trigger") // Log removal call
        _triggers.update { currentTriggers -> // Usar la función update
            currentTriggers.filter { it.uuid != trigger.uuid }
        }
        Timber.d("Trigger eliminado de la lista del ViewModel. Nueva lista size: ${_triggers.value.size}")
    }

    // Méto-do para actualizar un trigger existente
    fun updateTrigger(updatedTrigger: Trigger) {
        Timber.d("ViewModel: updateTrigger called with: $updatedTrigger") // Log update call
        _triggers.update { currentTriggers -> // Usar la función update
            val index = currentTriggers.indexOfFirst { it.uuid == updatedTrigger.uuid }
            if (index != -1) {
                // Crear una nueva lista con el elemento actualizado
                currentTriggers.toMutableList().apply { this[index] = updatedTrigger }.toList()
            } else {
                Timber.w("updateTrigger: Trigger with UUID ${updatedTrigger.uuid} not found in list.")
                currentTriggers // Devolver la lista actual si no se encontró
            }
        }
        Timber.d("updateTrigger: Trigger with UUID ${updatedTrigger.uuid} updated. New list size: ${_triggers.value.size}")
    }
}
