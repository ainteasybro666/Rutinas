package com.example.rutinas.data.repository

import androidx.room.withTransaction
import com.example.rutinas.data.local.AppDatabase
import com.example.rutinas.data.local.dao.ActionDao
import com.example.rutinas.data.local.dao.RoutineDao
import com.example.rutinas.data.local.dao.TriggerDao
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.RoutineEntity
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.domain.Routine
import com.example.rutinas.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : RoutineRepository {

    private val routineDao: RoutineDao = db.routineDao()
    private val triggerDao: TriggerDao = db.triggerDao()
    private val actionDao: ActionDao = db.actionDao()

    override fun getAllRoutines(): Flow<List<Routine>> {
        return routineDao.getRoutinesWithRelations()
            .map { routinesWithRelations ->
                routinesWithRelations.map { it.toDomainModel() }
            }
    }

    override suspend fun saveRoutine(routine: Routine): Resource<Long> = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                val routineId = if (routine.id == 0L) {
                    insertNewRoutine(routine)
                } else {
                    updateExistingRoutine(routine)
                }
                syncTriggers(routineId, routine.triggers)
                syncActions(routineId, routine.actions)
                Resource.Success(routineId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving routine")
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    private suspend fun insertNewRoutine(routine: Routine): Long {
        val routineEntity = RoutineEntity(
            uuid = UUID.randomUUID().toString(),
            name = routine.name,
            description = routine.description,
            isEnabled = routine.isEnabled,
            createdDate = routine.createdDate
        )
        return routineDao.insertRoutine(routineEntity)
    }

    private suspend fun updateExistingRoutine(routine: Routine): Long {
        routineDao.update(
            RoutineEntity(
                id = routine.id,
                uuid = routine.uuid,
                name = routine.name,
                description = routine.description,
                isEnabled = routine.isEnabled,
                createdDate = routine.createdDate
            )
        )
        return routine.id
    }

    private suspend fun syncTriggers(routineId: Long, triggers: List<Trigger>) {
        val currentTriggers = triggerDao.getTriggersForRoutine(routineId)
        // Eliminar triggers removidos
        currentTriggers.filter { current ->
            triggers.none { it.uuid == current.uuid }
        }.forEach { triggerDao.delete(it) }

        // Insertar/Actualizar triggers
        triggers.forEach { trigger ->
            val triggerToSave = trigger.copy(routineId = routineId)
            if (trigger.id == 0L) {
                triggerDao.insert(triggerToSave.copy(uuid = UUID.randomUUID().toString()))
            } else {
                triggerDao.update(triggerToSave)
            }
        }
    }

    private suspend fun syncActions(routineId: Long, actions: List<Action>) {
        val currentActions = actionDao.getActionsForRoutine(routineId)
        // Eliminar acciones removidas
        currentActions.filter { current ->
            actions.none { it.uuid == current.uuid }
        }.forEach { actionDao.delete(it) }

        // Insertar/Actualizar acciones
        actions.forEachIndexed { index, action ->
            val actionToSave = action.copy(routineId = routineId, executionOrder = index)
            if (action.id == 0L) {
                actionDao.insert(actionToSave.copy(uuid = UUID.randomUUID().toString()))
            } else {
                actionDao.update(actionToSave)
            }
        }
    }

    override suspend fun updateRoutineStatus(routineId: Long, isEnabled: Boolean) {
        withContext(Dispatchers.IO) {
            routineDao.updateEnabledStatus(routineId, isEnabled)
        }
    }

    override suspend fun insertRoutine(routine: Routine) {
        saveRoutine(routine)
    }

    override suspend fun updateActions(actions: List<Action>) {
        db.withTransaction {
            // No es necesario llamar a updateAll, ya que syncActions maneja las actualizaciones
        }
    }

    override suspend fun getRoutineByUuid(uuid: String): Routine? {
        return routineDao.getRoutineByUuid(uuid)?.let { routineEntity ->
            val triggers = triggerDao.getTriggersForRoutine(routineEntity.id)
            val actions = actionDao.getActionsForRoutine(routineEntity.id)
            routineEntity.toDomainModel(triggers, actions)
        }
    }

    override suspend fun updateActionsOrder(actions: List<Action>) {
        db.withTransaction {
            actions.forEachIndexed { index, action ->
                actionDao.updateActionOrder(action.uuid, index)
            }
        }
    }

    // Nuevos métodos para actualizar acciones individualmente y eliminar
    override suspend fun updateAction(action: Action) {
        withContext(Dispatchers.IO) {
            actionDao.update(action)
        }
    }

    override suspend fun deleteAction(action: Action) {
        withContext(Dispatchers.IO) {
            actionDao.delete(action)
        }
    }

    // Extension functions para conversión de modelos
    private fun AppDatabase.RoutineWithRelations.toDomainModel() = Routine(
        id = routine.id,
        uuid = routine.uuid,
        name = routine.name,
        description = routine.description,
        isEnabled = routine.isEnabled,
        createdDate = routine.createdDate,
        triggers = triggers,
        actions = actions.sortedBy { it.executionOrder }
    )

    private fun RoutineEntity.toDomainModel(
        triggers: List<Trigger>,
        actions: List<Action>
    ): Routine {
        return Routine(
            id = this.id,
            uuid = this.uuid,
            name = this.name,
            description = this.description,
            isEnabled = this.isEnabled,
            createdDate = this.createdDate,
            triggers = triggers,
            actions = actions.sortedBy { it.executionOrder }
        )
    }
}