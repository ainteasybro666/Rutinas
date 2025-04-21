package com.example.rutinasapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoutineRepository(private val routineDao: RoutineDao) {

    suspend fun getAllRoutines(): List<RoutineWithRelations> {
        return withContext(Dispatchers.IO) {
            routineDao.getAllRoutinesWithRelations()
        }
    }

    suspend fun getRoutine(routineId: Int): RoutineWithRelations {
        return withContext(Dispatchers.IO) {
            routineDao.getRoutineWithRelations(routineId)
        }
    }

    suspend fun insertRoutine(
        routine: RoutineEntity,
        triggers: List<TriggerEntity>,
        actions: List<ActionEntity>
    ): Long {
        return withContext(Dispatchers.IO) {
            val routineId = routineDao.insertRoutine(routine).toInt()
            triggers.forEach { it.copy(routineId = routineId) }
            actions.forEach { it.copy(routineId = routineId) }
            routineDao.insertTriggers(triggers)
            routineDao.insertActions(actions)
            routineId.toLong()
        }
    }

    suspend fun updateRoutine(
        routine: RoutineEntity,
        updatedTriggers: List<TriggerEntity>,
        updatedActions: List<ActionEntity>
    ) {
        withContext(Dispatchers.IO) {
            routineDao.updateRoutine(routine)

            // Borramos triggers/acciones anteriores si corresponde
            val existing = routineDao.getRoutineWithRelations(routine.id)
            routineDao.deleteTriggers(existing.triggers - updatedTriggers.toSet())
            routineDao.deleteActions(existing.actions - updatedActions.toSet())

            // Insertamos/actualizamos las nuevas listas
            routineDao.insertTriggers(updatedTriggers)
            routineDao.insertActions(updatedActions)
        }
    }

    suspend fun deleteRoutine(routine: RoutineEntity) {
        withContext(Dispatchers.IO) {
            routineDao.deleteRoutine(routine)
        }
    }
}