package com.example.rutinas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.data.model.Routine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject @HiltViewModel

class RoutineViewModel @Inject constructor(
    private val routineRepository: RoutineRepository
) : ViewModel() {
    // Convertimos el Flow del repositorio a un StateFlow, para que la UI lo observe directamente.
    val routines: StateFlow<List<Routine>> = routineRepository.getAllRoutines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Propiedades para “nuevo” / “edición” como antes:
    private val _routineName = MutableStateFlow("")
    val routineName: StateFlow<String> get() = _routineName

    private val _triggerType = MutableStateFlow("TIME")
    val triggerType: StateFlow<String> get() = _triggerType

    private val _triggerData = MutableStateFlow("")
    val triggerData: StateFlow<String> get() = _triggerData

    private val _actions = MutableStateFlow<List<String>>(emptyList())
    val actions: StateFlow<List<String>> get() = _actions

    private val _selectedFrequency = MutableStateFlow("Once")
    val selectedFrequency: StateFlow<String> get() = _selectedFrequency

    private val _selectedDayType = MutableStateFlow("None")
    val selectedDayType: StateFlow<String> get() = _selectedDayType

    private val _selectedDayValue = MutableStateFlow("")
    val selectedDayValue: StateFlow<String> get() = _selectedDayValue

    private val _notify5MinBefore = MutableStateFlow(false)
    val notify5MinBefore: StateFlow<Boolean> get() = _notify5MinBefore

    private val _isActive = MutableStateFlow(true)
    val isActive: StateFlow<Boolean> get() = _isActive

    private val _actionType = MutableStateFlow("ALARM")
    val actionType: StateFlow<String> get() = _actionType

    private val _actionData = MutableStateFlow("")
    val actionData: StateFlow<String> get() = _actionData

// En vez de inicializar la lista con datos de ejemplo, ya no lo hacemos,
// pues la consulta se hace a través de routineRepository.getAllRoutines().

    // Setters...
    fun setRoutineName(name: String) { _routineName.value = name }
    fun setTriggerType(tt: String) { _triggerType.value = tt }
    fun setTriggerData(td: String) { _triggerData.value = td }
    fun setSelectedFrequency(freq: String) { _selectedFrequency.value = freq }
    fun setSelectedDayType(dayType: String) { _selectedDayType.value = dayType }
    fun setSelectedDayValue(dayValue: String) { _selectedDayValue.value = dayValue }
    fun setNotify5MinBefore(value: Boolean) { _notify5MinBefore.value = value }
    fun setIsActive(value: Boolean) { _isActive.value = value }
    fun setActionType(aType: String) { _actionType.value = aType }
    fun setActionData(aData: String) { _actionData.value = aData }

    fun addAction(actionName: String) {
        _actions.value = _actions.value + actionName
    }

    fun moveActionUp(index: Int) {
        if (index <= 0) return
        val current = _actions.value.toMutableList()
        val item = current.removeAt(index)
        current.add(index - 1, item)
        _actions.value = current
    }

    fun moveActionDown(index: Int) {
        val current = _actions.value.toMutableList()
        if (index < 0 || index >= current.lastIndex) return
        val item = current.removeAt(index)
        current.add(index + 1, item)
        _actions.value = current
    }

    // Guardar la rutina en la base de datos
    fun saveRoutine(routineId: Long?) {
        viewModelScope.launch {
            val newId = routineId ?: System.currentTimeMillis() // O autoGenerate en BD
            val newRoutine = Routine(
                id = 0, // En Room con autoGenerate, se ignora y se asigna uno nuevo.
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
            // Si routineId != null, significa que es una actualización
            if (routineId != null) {
                // Hasta podrías usar updateRoutine() en vez de insertRoutine()
                // si quieres diferenciar.
                routineRepository.insertRoutine(newRoutine)
            } else {
                routineRepository.insertRoutine(newRoutine)
            }
        }
    }
    fun updateTimeTrigger(time: String) {
// Asignas el tipo de disparador como TIME
        _triggerType.value = "TIME"
// Guardas la hora en el triggerData
        _triggerData.value = time
    }
}