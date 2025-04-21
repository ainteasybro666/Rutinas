package com.example.rutinasapp.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rutinasapp.data.*
import kotlinx.coroutines.launch

class RoutinesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = RoutineRepository(db.routineDao())

    private val _routinesList = MutableLiveData<List<RoutineWithRelations>>()
    val routinesList: LiveData<List<RoutineWithRelations>> get() = _routinesList

    // Cargamos las rutinas al iniciar
    init {
        loadRoutines()
    }

    fun loadRoutines() {
        viewModelScope.launch {
            val data = repository.getAllRoutines()
            _routinesList.value = data
        }
    }

    fun insertRoutine(
        name: String,
        isActive: Boolean,
        triggers: List<TriggerEntity>,
        actions: List<ActionEntity>
    ) {
        viewModelScope.launch {
            repository.insertRoutine(
                RoutineEntity(name = name, isActive = isActive),
                triggers,
                actions
            )
            loadRoutines() // recarga la lista
        }
    }

    fun updateRoutine(
        routineId: Int,
        name: String,
        isActive: Boolean,
        triggers: List<TriggerEntity>,
        actions: List<ActionEntity>
    ) {
        viewModelScope.launch {
            repository.updateRoutine(
                RoutineEntity(id = routineId, name = name, isActive = isActive),
                triggers,
                actions
            )
            loadRoutines()
        }
    }
}