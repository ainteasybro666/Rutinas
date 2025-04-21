package com.example.rutinas.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.asLiveData
import javax.inject.Inject

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    private val repository: RoutineRepository
) : ViewModel() {

    // Usamos directamente el Flow convertido a LiveData.
    val routines: LiveData<List<Routine>> = repository.getAllRoutines().asLiveData()

    fun toggleRoutineStatus(routineId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateRoutineStatus(routineId, isEnabled)
        }
    }

    fun insertRoutine(routine: Routine) {
        viewModelScope.launch {
            repository.insertRoutine(routine)
        }
    }
}
