package com.example.rutinas.data.repository

import com.example.rutinas.data.dao.RoutineDao
import com.example.rutinas.data.model.Routine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RoutineRepository @Inject constructor(
    private val routineDao: RoutineDao
) {
    fun getAllRoutines(): Flow<List<Routine>> = routineDao.getAllRoutines()

    suspend fun getRoutineById(id: Long): Flow<Routine?> = routineDao.getRoutineById(id)

    suspend fun insertRoutine(routine: Routine): Long = routineDao.insertRoutine(routine)

    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: Routine) = routineDao.deleteRoutine(routine)
}