package com.example.rutinas.data.repository

import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.domain.Routine
import com.example.rutinas.utils.Resource
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getAllRoutines(): Flow<List<Routine>>
    suspend fun updateRoutineStatus(routineId: Long, isEnabled: Boolean)
    suspend fun insertRoutine(routine: Routine): Long // Asume que devuelve el ID insertado
    suspend fun updateActions(actions: List<Action>)
    suspend fun getRoutineByUuid(uuid: String): Routine?
    suspend fun updateActionsOrder(actions: List<Action>)
    suspend fun updateAction(action: Action)
    suspend fun deleteAction(action: Action)
    suspend fun updateRoutine(routine: Routine)
    suspend fun deleteRoutine(routine: Routine)
    suspend fun getRoutineById(id: Long): Routine?
    suspend fun getTriggersForRoutine(routineId: Long): List<Trigger>
}