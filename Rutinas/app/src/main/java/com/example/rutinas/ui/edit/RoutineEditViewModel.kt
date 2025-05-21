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
import timber.log.Timber.Forest.e
import java.time.LocalDateTime
import java.util.Collections.frequency
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

    // Flujo para eventos de UI (como mostrar Toasts)
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // Clase sealed para definir los tipos de eventos de UI
    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        // Puedes añadir otros eventos aquí, ej: NavigateBack, ShowLoading, HideLoading
    }

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
            for (trigger in _triggers.value) {
                if (trigger.triggerType == "TIME") {
                    // Obtener los datos de forma segura
                    val frequency = trigger.data.data["frequency"] as? String
                    val daysOfWeek = trigger.data.data["daysOfWeek"] as? List<Int>
                    val dayOfMonth = trigger.data.data["dayOfMonth"] as? Int

                    // ** VALIDACIÓN PARA TRIGGER SEMANAL - DEBE ESTAR AQUÍ DENTRO DEL BLOQUE TIME **
                    if (frequency == "weekly") {
                        Timber.d("Validando trigger semanal.")
                        if (daysOfWeek.isNullOrEmpty()) {
                            Timber.d("Validación: Trigger semanal sin días de la semana.")
                            viewModelScope.launch { // <-- Este launch está dentro del launch principal
                                _uiEvent.emit(UiEvent.ShowMessage("Debes seleccionar al menos un día de la semana para un trigger semanal.")) // Usa UiEvent
                            }
                            _saveResult.emit(Resource.Error("Debes seleccionar al menos un día de la semana para un trigger semanal."))
                            return@launch // <-- return@launch sale del launch principal
                        }
                        Timber.d("Validación: Trigger semanal OK.")
                    }

                    // ** VALIDACIÓN PARA TRIGGER MENSUAL - DEBE ESTAR AQUÍ DENTRO DEL BLOQUE TIME **
                    if (frequency == "monthly") {
                        Timber.d("Validando trigger mensual.")
                        if (dayOfMonth == null || dayOfMonth !in 1..31) {
                            Timber.d("Validación: Trigger mensual con día del mes inválido.")
                            viewModelScope.launch { // <-- Este launch está dentro del launch principal
                                _uiEvent.emit(UiEvent.ShowMessage("El día del mes debe estar entre 1 y 31 para un trigger mensual.")) // Usa UiEvent
                            }
                            _saveResult.emit(Resource.Error("El día del mes debe estar entre 1 y 31 para un trigger mensual."))
                            return@launch // <-- return@launch sale del launch principal
                        }
                        Timber.d("Validación: Trigger mensual OK.")
                    }

                    // Puedes añadir validaciones para otros tipos de frecuencia si existen (ej. "daily", "once")
                    // ** VALIDACIÓN PARA FRECUENCIA NO VÁLIDA - DEBE ESTAR AQUÍ DENTRO DEL BLOQUE TIME **
                    if (frequency == null || (frequency != "weekly" && frequency != "monthly" /* && otros tipos */)) {
                        Timber.d("Validación: Frecuencia de trigger de tiempo no válida.")
                        viewModelScope.launch { // <-- Este launch está dentro del launch principal
                            _uiEvent.emit(UiEvent.ShowMessage("Frecuencia del trigger de tiempo no válida.")) // Usa UiEvent
                        }
                        _saveResult.emit(Resource.Error("Frecuencia del trigger de tiempo no válida."))
                        return@launch // <-- return@launch sale del launch principal
                    }
                    Timber.d("Validación: Frecuencia de trigger de tiempo OK.")

                } // <-- Fin del bloque if (trigger.triggerType == "TIME")

                // ** VALIDACIONES PARA OTROS TIPOS DE TRIGGERS - DEBEN ESTAR AQUÍ DENTRO DEL BUCLE FOR **
                if (trigger.triggerType == "CALENDAR") {
                    Timber.d("Validando trigger de calendario. Data: ${trigger.data.data}")
                    val calendarTime = trigger.data.data["calendarTime"] as? LocalDateTime
                    if (calendarTime == null) {
                        Timber.d("Validación: Trigger de calendario sin fecha/hora.")
                        viewModelScope.launch { // <-- Este launch está dentro del launch principal
                            _uiEvent.emit(UiEvent.ShowMessage("La fecha y hora del trigger de calendario no pueden estar vacías.")) // Usa UiEvent
                        }
                        _saveResult.emit(Resource.Error("La fecha y hora del trigger de calendario no pueden estar vacías."))
                        return@launch // <-- return@launch sale del launch principal
                    }
                    Timber.d("Validación: Trigger de calendario OK.")
                }
                    // Podrías validar que calendarTime.isAfter(LocalDateTime.now()) si tiene sentido

                // Puedes añadir más validaciones para otros tipos de triggers aquí
//            }
//            if (trigger.triggerType == "LOCATION") {
//                Timber.d("Validando trigger de ubicación. Data: ${trigger.data.data}")
//                val latitude = trigger.data.data["latitude"] as? Double
//                val longitude = trigger.data.data["longitude"] as? Double
//                val radius = trigger.data.data["radius"] as? Double
//                val locationName = trigger.data.data["locationName"] as? String
//                val enterExit = trigger.data.data["enterExit"] as? String
//
//                if (latitude == null || longitude == null || radius == null || locationName.isNullOrEmpty() || enterExit.isNullOrEmpty()) {
//                    Timber.d("Validación: Trigger de ubicación incompleto.")
//                    viewModelScope.launch { // <-- Este launch está dentro del launch principal
//                        _uiEvent.emit(UiEvent.ShowMessage("La configuración del trigger de ubicación está incompleta.")) // Usa UiEvent
//                    }
//                    _saveResult.emit(Resource.Error("La configuración del trigger de ubicación está incompleta."))
//                    return@launch // <-- return@launch sale del launch principal
//                }
//                if (enterExit != "enter" && enterExit != "exit") {
//                    Timber.d("Validación: Trigger de ubicación con tipo de evento inválido.")
//                    viewModelScope.launch { // <-- Este launch está dentro del launch principal
//                        _uiEvent.emit(UiEvent.ShowMessage("El tipo de evento (entrada/salida) del trigger de ubicación no es válido.")) // Usa UiEvent
//                    }
//                    _saveResult.emit(Resource.Error("El tipo de evento (entrada/salida) del trigger de ubicación no es válido."))
//                    return@launch // <-- return@launch sale del launch principal
//                }
//                Timber.d("Validación: Trigger de ubicación OK.")
//            }
            }

            // ** COMIENZO DEL BLOQUE TRY PARA GUARDAR LA RUTINA Y SUS TRIGGERS/ACTIONS **
            try {
                Timber.d("ViewModel: Pasó validaciones de triggers. Procediendo a guardar en BD.")
                // Crear una nueva Routine con los datos actualizados
                val routineToSave = currentRoutine.copy(
                    name = name,
                    triggers = _triggers.value.toList(),
                    actions = _actions.value.toList()
                )

                val resultId = if (isNewRoutine) {
                    val insertedId = repository.insertRoutine(routineToSave)
                    Timber.d("ViewModel: Rutina insertada con ID: $insertedId")
                    insertedId
                } else {
                    Timber.d("ViewModel: Actualizando rutina existente con ID: ${routineToSave.id}")
                    repository.updateRoutine(routineToSave)
                    routineToSave.id
                }

                _saveResult.emit(Resource.Success(resultId))

                if (isNewRoutine) {
                    Timber.d("Updating _currentRoutine with new ID: $resultId")
                    _currentRoutine.value = routineToSave.copy(id = resultId, uuid = routineToSave.uuid)
                    _triggers.update { currentList -> currentList.map { it.copy(routineId = resultId) }.toList() }
                    _actions.update { currentList -> currentList.map { it.copy(routineId = resultId) }.toList() }
                    isNewRoutine = false
                }

                Timber.d("Rutina guardada con éxito, ID: $resultId")

            } catch (e: Exception) { // <-- Bloque catch del try
                Timber.e("Error inesperado al guardar rutina: ${e.message}", e)
                viewModelScope.launch { // <-- Este launch está dentro del launch principal
                    _uiEvent.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Error desconocido al guardar rutina")) // Usa UiEvent
                }
                _saveResult.emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
            }
        }
    }

    // Modificado para usar listas inmutables, generar UUID y devolver el trigger añadido
    fun addTrigger(trigger: Trigger): Resource<Trigger> { // Devuelve Resource<Trigger> para indicar éxito o error
        Timber.d("ViewModel: addTrigger called with: ${trigger.triggerType}") // Log add call

        // ** NEW: Check for existing trigger of the same type **
        val existingTrigger = _triggers.value.find { it.triggerType == trigger.triggerType }
        if (existingTrigger != null) {
                val errorMessage = "Ya existe un trigger de tipo ${trigger.triggerType} para esta rutina. Solo se permite uno de cada tipo."
                Timber.w("Attempted to add duplicate trigger type: ${trigger.triggerType}")
                return Resource.Error(errorMessage) // Return an error resource
        }

        // ** END NEW **
        val triggerToAdd = if (trigger.uuid.isEmpty() || trigger.uuid == "0") { // Consider "0" as well if that's a possible initial state
                 trigger.copy(uuid = UUID.randomUUID().toString())
             } else {
                 trigger
             }

        
        // Crear una nueva lista con el trigger añadido y emitir
        _triggers.update { currentTriggers ->
            currentTriggers + triggerToAdd
        }
        Timber.d("Trigger añadido a la lista del ViewModel: ${triggerToAdd.uuid}. Lista total size: ${_triggers.value.size}")
        return Resource.Success(triggerToAdd) // Return a success resource with the added trigger
    }

    fun saveTrigger(trigger: Trigger) {
        Timber.d("ViewModel: saveTrigger called with trigger: ${trigger.uuid}")

        // Verificar si el trigger con este UUID ya existe en la lista actual
        val existingTriggerIndex = _triggers.value.indexOfFirst { it.uuid == trigger.uuid }

        if (existingTriggerIndex != -1) {
            // El trigger YA existe, es una ACTUALIZACIÓN
            Timber.d("ViewModel: Trigger with UUID ${trigger.uuid} found. Updating...")
            updateTrigger(trigger) // Llama al método updateTrigger existente
            //updateTrigger ya maneja la emisión al StateFlow
        } else {
            // El trigger NO existe (o tiene UUID vacío/0), es uno NUEVO
            Timber.d("ViewModel: Trigger with UUID ${trigger.uuid} not found. Adding...")

            // Tu addTrigger actual tiene la lógica de validar duplicados por TIPO.
            // Y asigna un UUID si está vacío.
            // Así que podemos seguir usando addTrigger para la lógica de "añadir uno nuevo".
            // addTrigger devuelve un Resource<Trigger> para indicar si fue exitoso o si hubo duplicado.

            val addResult = addTrigger(trigger) // Llama al método addTrigger existente

            when(addResult) {
                is Resource.Success -> {
                    Timber.d("ViewModel: Trigger added successfully with UUID: ${addResult.data.uuid}")
                    // addTrigger ya emite la lista actualizada al StateFlow
                }
                is Resource.Error -> {
                    Timber.e("ViewModel: Failed to add trigger: ${addResult.message}")
                    // Opcional: Emitir un evento de UI para mostrar el error al usuario
                    viewModelScope.launch {
                        _uiEvent.emit(RoutineEditViewModel.UiEvent.ShowMessage(addResult.message ?: "Error desconocido al añadir trigger"))
                        // Asegúrate de que tienes el uiEvent flow en tu ViewModel (Opción 1)
                        // _uiEvent.emit(UiEvent.ShowMessage(addResult.message ?: "Error desconocido al añadir trigger"))
                        // Si no tienes uiEvent, maneja el error de otra manera (ej. log)
                    }
                }
                else -> { /* Resource.Loading - no relevante para la respuesta sincrónica de addTrigger */ }
            }
        }
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
