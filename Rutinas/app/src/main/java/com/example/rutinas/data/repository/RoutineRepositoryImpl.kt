package com.example.rutinas.data.repository

import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.local.dao.ActionDao
import com.example.rutinas.data.local.dao.RoutineDao
import com.example.rutinas.data.local.dao.TriggerDao
import com.example.rutinas.data.local.AppDatabase // Importa AppDatabase para RoutineWithRelations
import com.example.rutinas.data.model.RoutineEntity // Importa RoutineEntity
import com.example.rutinas.data.model.Action // Importa Action (es una entidad y modelo)
import com.example.rutinas.data.model.Trigger // Importa Trigger (es una entidad y modelo)
import com.example.rutinas.domain.Routine // Importa tu modelo de dominio Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao,
    private val actionDao: ActionDao,
    private val triggerDao: TriggerDao,
    private val alarmScheduler: AlarmScheduler // <<-- NUEVO: Inyectar AlarmScheduler
    // Otros DAOs si los tienes
) : RoutineRepository {

    override fun getAllRoutines(): Flow<List<Routine>> {
        Timber.d("Repository: Getting all routines with relations")
        return routineDao.getRoutinesWithRelations().map { list ->
            // Mapea la lista de RoutineWithRelations a una lista de modelos de dominio Routine
            list.map { it.toRoutineDomain() } // Usa una función de mapeo interna o de extensión
        }
    }

    override suspend fun updateRoutineStatus(routineId: Long, isEnabled: Boolean) {
        Timber.d("Repository: Updating routine status for ID: $routineId to $isEnabled")
        routineDao.updateEnabledStatus(routineId, isEnabled)
        Timber.d("Repository: Routine status updated for ID: $routineId")

        // <<-- NUEVO: Después de actualizar el estado, recalendarizar la rutina -->>
        // Necesitamos la rutina completa para recalendarizar. La cargamos por ID.
        val routine = routineDao.getRoutineById(routineId)?.toRoutineDomain(
            triggerDao.getTriggersForRoutine(routineId),
            actionDao.getActionsForRoutine(routineId)
        )
        if (routine != null) {
            if (routine.isEnabled) {
                alarmScheduler.schedule(routine)
                Timber.d("Repository: Scheduled routine with ID: $routineId after status update")
            } else {
                alarmScheduler.cancel(routine)
                Timber.d("Repository: Canceled routine with ID: $routineId after status update")
            }
        } else {
            Timber.e("Repository: Could not find routine with ID: $routineId after status update")
        }
        // <<-- FIN NUEVO -->>
    }

    override suspend fun insertRoutine(routine: Routine): Long {
        Timber.d("Repository: Inserting routine: ${routine.name}")

        val routineEntity = routine.toRoutineEntity()
        val routineId = routineDao.insertRoutine(routineEntity)
        Timber.d("Repository: Routine inserted with ID: $routineId")

        val triggersToInsert = routine.triggers.map { it.copy(routineId = routineId) }
        triggersToInsert.forEach { triggerDao.insertTrigger(it) }

        val actionsToInsert = routine.actions.map { it.copy(routineId = routineId) }
        actionsToInsert.forEach { actionDao.insert(it) }

        Timber.d("Repository: Triggers and actions inserted for routine ID: $routineId")

        // <<-- NUEVO: Después de insertar, programar las alarmas para esta rutina -->>
        // Creamos un objeto Routine completo para pasárselo al scheduler
        val insertedRoutine = routine.copy(id = routineId, triggers = triggersToInsert, actions = actionsToInsert)
        if (insertedRoutine.isEnabled) { // Solo programar si la rutina está habilitada por defecto (o según el modelo)
            alarmScheduler.schedule(insertedRoutine)
            Timber.d("Repository: Scheduled routine with ID: $routineId after insertion")
        } else {
            Timber.d("Repository: Routine with ID: $routineId is disabled, skipping initial scheduling")
        }

        // <<-- FIN NUEVO -->>

        return routineId
    }

    override suspend fun updateActions(actions: List<Action>) {
        Timber.d("Repository: Updating a list of actions (${actions.size}) individually")
        actions.forEach { action ->
            actionDao.update(action)
        }
        Timber.d("Repository: List of actions updated individually")
    }

    override suspend fun getRoutineByUuid(uuid: String): Routine? {
        Timber.d("Repository: Getting routine by UUID: $uuid")
        val routineEntity = routineDao.getRoutineByUuid(uuid)

        return routineEntity?.let { entity ->
            val triggers = triggerDao.getTriggersForRoutine(entity.id)
            val actions = actionDao.getActionsForRoutine(entity.id)

            entity.toRoutineDomain(triggers, actions)
        } ?: run {
            Timber.d("Repository: Routine with UUID: $uuid not found")
            null
        }
    }

    override suspend fun updateActionsOrder(actions: List<Action>) {
        Timber.d("Repository: Updating actions order for ${actions.size} actions")
        actions.forEach { action ->
            actionDao.updateActionOrder(action.uuid, action.executionOrder)
        }
        Timber.d("Repository: Actions order updated")

        // <<-- OPCIONAL: Reprogramar si el orden de las acciones afecta triggers (poco probable para tiempo/calendario) -->>
        // Para triggers de tiempo y calendario, el orden de las acciones no afecta cuándo se dispara el trigger,
        // así que probablemente no necesites reprogramar aquí. Dejo el comentario por si acaso.
        // Si tuvieras un trigger que dependiera de la finalización de acciones previas, SÍ deberías reprogramar.
    }

    override suspend fun updateAction(action: Action) {
        Timber.d("Repository: Updating action with UUID: ${action.uuid}")
        actionDao.update(action)
        Timber.d("Repository: Action updated with UUID: ${action.uuid}")

        // <<-- OPCIONAL: Reprogramar si actualizar una acción afecta triggers (poco probable para tiempo/calendario) -->>
        // Similar al caso anterior, actualizar una acción individual (ej: cambiar volumen) no debería afectar cuándo
        // se dispara un trigger de tiempo o calendario.
    }

    override suspend fun deleteAction(action: Action) {
        Timber.d("Repository: Deleting action with UUID: ${action.uuid}")
        actionDao.delete(action)
        Timber.d("Repository: Action deleted with UUID: ${action.uuid}")

        // <<-- OPCIONAL: Reprogramar si eliminar una acción afecta triggers -->>
        // Igual que actualizar, poco probable que afecte triggers de tiempo/calendario.
    }

    override suspend fun updateRoutine(routine: Routine) {
        Timber.d("Repository: Updating routine: ${routine.name} with ID: ${routine.id}")

        // <<-- NUEVO: Antes de actualizar, cancelar las alarmas existentes para esta rutina -->>
        // Esto es importante para evitar duplicados o alarmas obsoletas.
        // Necesitamos la rutina *antes* de la actualización para cancelar correctamente.
        val oldRoutine = getRoutineByUuid(routine.uuid) // Cargamos la versión anterior
        if (oldRoutine != null) {
            alarmScheduler.cancel(oldRoutine)
            Timber.d("Repository: Canceled old alarms for routine with ID: ${routine.id} before update")
        } else {
            Timber.w("Repository: Could not find old routine with UUID: ${routine.uuid} to cancel alarms before update.")
            // Considerar si emitir un error o manejarlo de otra forma si la rutina vieja no se encuentra.
        }
        // <<-- FIN NUEVO -->>


        routineDao.update(routine.toRoutineEntity())
        Timber.d("Repository: Main routine entity updated")

        triggerDao.deleteTriggersForRoutine(routine.id)
        actionDao.deleteActionsForRoutine(routine.id)
        Timber.d("Repository: Deleted old triggers and actions for routine ID: ${routine.id}")

        routine.triggers.forEach { triggerDao.insertTrigger(it) }
        routine.actions.forEach { actionDao.insert(it) }
        Timber.d("Repository: Inserted updated triggers and actions for routine ID: ${routine.id}")

        Timber.d("Repository: Routine, triggers, and actions updated for ID: ${routine.id}")

        // <<-- NUEVO: Después de actualizar, programar las nuevas alarmas para esta rutina -->>
        // Usamos el objeto 'routine' que ya tiene los triggers y actions actualizados.
        if (routine.isEnabled) { // Solo programar si la rutina está habilitada
            alarmScheduler.schedule(routine)
            Timber.d("Repository: Scheduled routine with ID: ${routine.id} after update")
        } else {
            Timber.d("Repository: Routine with ID: ${routine.id} is disabled after update, skipping scheduling")
        }
        // <<-- FIN NUEVO -->>
    }

    // NEW: Implement the deleteRoutine function
    override suspend fun deleteRoutine(routine: Routine) {
        Timber.d("Repository: Deleting routine with ID: ${routine.id} and UUID: ${routine.uuid}")

        // <<-- NUEVO: Antes de eliminar, cancelar las alarmas existentes para esta rutina -->>
        alarmScheduler.cancel(routine)
        Timber.d("Repository: Canceled alarms for routine with ID: ${routine.id} before deletion")
        // <<-- FIN NUEVO -->>

        Timber.d("Repository: Deleting triggers for routine ID: ${routine.id}")
        triggerDao.deleteTriggersForRoutine(routine.id)

        Timber.d("Repository: Deleting actions for routine ID: ${routine.id}")
        actionDao.deleteActionsForRoutine(routine.id)

        Timber.d("Repository: Deleting the routine entity for ID: ${routine.id}")
        routineDao.deleteRoutine(routine.toRoutineEntity())

        Timber.d("Repository: Routine and associated data deleted successfully for ID: ${routine.id}")
    }


    // Implementa otras funciones de la interfaz RoutineRepository si hay más.\n\n
}

// --- Funciones de Mapeo ---
// Estas funciones convierten entre tus entidades de Room (RoutineEntity, RoutineWithRelations)
// y tu modelo de dominio (Routine). Puedes colocarlas aquí o en un archivo de extensiones.

// Mapeo de RoutineWithRelations a tu modelo de dominio Routine
fun AppDatabase.RoutineWithRelations.toRoutineDomain(): Routine {
    return Routine(
        id = this.routine.id,
        uuid = this.routine.uuid,
        name = this.routine.name,
        description = this.routine.description,
        isEnabled = this.routine.isEnabled,
        createdDate = this.routine.createdDate,
        triggers = this.triggers, // Usa las listas de la relación
        actions = this.actions // Usa las listas de la relación
    )
}

// Mapeo de tu modelo de dominio Routine a RoutineEntity (para guardar la entidad principal)
fun Routine.toRoutineEntity(): RoutineEntity {
    return RoutineEntity(
        id = this.id, // Room usará 0 para entidades nuevas con primary key autogenerada
        uuid = this.uuid,
        name = this.name,
        description = this.description,
        isEnabled = this.isEnabled,
        createdDate = this.createdDate
    )
}

// Mapeo de RoutineEntity (cuando se carga individualmente) a tu modelo de dominio Routine
// Este se usa cuando no cargas las relaciones automáticamente (ej: getRoutineByUuid)
fun RoutineEntity.toRoutineDomain(triggers: List<Trigger> = emptyList(), actions: List<Action> = emptyList()): Routine {
    return Routine(
        id = this.id,
        uuid = this.uuid,
        name = this.name,
        description = this.description,
        isEnabled = this.isEnabled,
        createdDate = this.createdDate,
        triggers = triggers, // Añade las listas cargadas por separado
        actions = actions // Añade las listas cargadas por separado
    )
}
