package com.example.rutinas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinas.data.model.Routine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoutineViewModel : ViewModel() {

    // Lista global de rutinas de ejemplo (podrías sustituir por DB).
    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines

    // Nombre
    private val _routineName = MutableStateFlow("")
    val routineName: StateFlow<String> get() = _routineName

    // Tipo y data del trigger (p.ej. "TIME" y "08:00")
    private val _triggerType = MutableStateFlow("TIME")
    val triggerType: StateFlow<String> get() = _triggerType

    private val _triggerData = MutableStateFlow("")
    val triggerData: StateFlow<String> get() = _triggerData

    // Acciones
    private val _actions = MutableStateFlow<List<String>>(emptyList())
    val actions: StateFlow<List<String>> get() = _actions

    // Frecuencia (“Once”, “Daily”, “Weekly”, “Monthly”)
    private val _selectedFrequency = MutableStateFlow("Once")
    val selectedFrequency: StateFlow<String> get() = _selectedFrequency

    // Tipo de día (“None”, “Weekday”, “Date”)
    private val _selectedDayType = MutableStateFlow("None")
    val selectedDayType: StateFlow<String> get() = _selectedDayType

    // Valor del día (ej. “Monday” o “15”)
    private val _selectedDayValue = MutableStateFlow("")
    val selectedDayValue: StateFlow<String> get() = _selectedDayValue

    // Notificar 5 min antes
    private val _notify5MinBefore = MutableStateFlow(false)
    val notify5MinBefore: StateFlow<Boolean> get() = _notify5MinBefore

    // Activar/desactivar
    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> get() = _isActive

    // Por si usas “actionType” y “actionData”.
    private val _actionType = MutableStateFlow("ALARM")
    val actionType: StateFlow<String> get() = _actionType

    private val _actionData = MutableStateFlow("")
    val actionData: StateFlow<String> get() = _actionData

    // Simulamos inicializar lista de rutinas
    init {
        _routines.value = listOf(
            Routine(
                id = 1,
                name = "Rutina Matinal",
                triggerType = "TIME",
                triggerData = "07:00",
                actionType = "ALARM",
                actionData = "Volumen alto",
                frequency = "Daily"
            ),
            Routine(
                id = 2,
                name = "Rutina Wifi",
                triggerType = "WIFI",
                triggerData = "MiRedWifi",
                actionType = "BRIGHTNESS",
                actionData = "Bajar a 50%",
                frequency = "Once",
                dayType = "None",
                isActive = true
            )
        )
    }

    // Setters para cada StateFlow
    fun setRoutineName(name: String) {
        _routineName.value = name
    }

    fun setTriggerType(tt: String) {
        _triggerType.value = tt
    }

    fun setTriggerData(td: String) {
        _triggerData.value = td
    }

    fun setSelectedFrequency(freq: String) {
        _selectedFrequency.value = freq
    }

    fun setSelectedDayType(dayType: String) {
        _selectedDayType.value = dayType
    }

    fun setSelectedDayValue(dayValue: String) {
        _selectedDayValue.value = dayValue
    }

    fun setNotify5MinBefore(value: Boolean) {
        _notify5MinBefore.value = value
    }

    fun setIsActive(value: Boolean) {
        _isActive.value = value
    }

    fun setActionType(aType: String) {
        _actionType.value = aType
    }

    fun setActionData(aData: String) {
        _actionData.value = aData
    }

    // Para actualizar la hora en el “triggerData”
    fun updateTimeTrigger(time: String) {
        _triggerType.value = "TIME"
        _triggerData.value = time
    }

    // Manejo de acciones como lista
    fun addAction(actionName: String) {
        _actions.value = _actions.value + actionName
    }

    fun moveActionUp(index: Int) {
        if (index <= 0) return
        val currentList = _actions.value.toMutableList()
        val item = currentList.removeAt(index)
        currentList.add(index - 1, item)
        _actions.value = currentList
    }

    fun moveActionDown(index: Int) {
        val currentList = _actions.value.toMutableList()
        if (index < 0 || index >= currentList.lastIndex) return
        val item = currentList.removeAt(index)
        currentList.add(index + 1, item)
        _actions.value = currentList
    }

    // Guardar la rutina (crear o actualizar)
    fun saveRoutine(routineId: Long?) {
        viewModelScope.launch {
            val newId = routineId ?: System.currentTimeMillis()
            val newRoutine = Routine(
                id = newId,
                name = _routineName.value,
                triggerType = _triggerType.value,
                triggerData = _triggerData.value,
                frequency = _selectedFrequency.value,
                dayType = _selectedDayType.value,
                dayValue = _selectedDayValue.value,
                notify5MinBefore = _notify5MinBefore.value,
                actions = _actions.value,
                isActive = _isActive.value,
                actionType = _actionType.value,
                actionData = _actionData.value
            )
            val oldList = _routines.value.toMutableList()
            val index = oldList.indexOfFirst { it.id == newId }
            if (index != -1) {
                // Actualizar
                oldList[index] = newRoutine
            } else {
                // Crear
                oldList.add(newRoutine)
            }
            _routines.value = oldList
        }
    }
}