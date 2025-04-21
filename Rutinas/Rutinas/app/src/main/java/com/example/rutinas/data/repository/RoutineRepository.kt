package com.example.rutinas.data.repository

import com.example.rutinas.data.model.Action
import com.example.rutinas.domain.Routine
import com.example.rutinas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getAllRoutines(): Flow<List<Routine>>
    suspend fun saveRoutine(routine: Routine): Resource<Long>
    suspend fun updateRoutineStatus(routineId: Long, isEnabled: Boolean)
    suspend fun insertRoutine(routine: Routine)
    suspend fun updateActions(actions: List<Action>)
    suspend fun getRoutineByUuid(uuid: String): Routine?
    suspend fun updateActionsOrder(actions: List<Action>)
}