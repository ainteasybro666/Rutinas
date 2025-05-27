package com.example.rutinas.data.repository

import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.local.dao.ActionDao
import com.example.rutinas.data.local.dao.RoutineDao
import com.example.rutinas.data.local.dao.TriggerDao
import com.example.rutinas.data.local.AppDatabase
import com.example.rutinas.data.model.RoutineEntity
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.domain.Routine
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
    //private val alarmScheduler: AlarmScheduler
    // Otros DAOs si los tienes
) : RoutineRepository {

    override fun getAllRoutines(): Flow<List<Routine>> {
        Timber.d("Repository: Getting all routines with relations")
        return routineDao.getRoutinesWithRelations().map { list ->
            // Mapea la lista de RoutineWithRelations a una lista de modelos de dominio Routine
            list.map { it.toRoutineDomain() } // Usa una función de mapeo interna o de extensión
        }
    }

    // <<-- IMPLEMENTACIÓN DE getRoutineById -->>
    override suspend fun getRoutineById(id: Long): Routine? {
        Timber.d("Repository: Getting routine by ID: $id")
        // Obtener la entidad principal
        val routineEntity = routineDao.getRoutineById(id)

        // Si se encuentra la entidad, cargar las relaciones y mapear a dominio
        return routineEntity?.let { entity ->
            val triggers = triggerDao.getTriggersForRoutine(entity.id)
            val actions = actionDao.getActionsForRoutine(entity.id)
            entity.toRoutineDomain(triggers, actions)
        } ?: run {
            Timber.d("Repository: Routine with ID: $id not found")
            null
        }
    }
    // <<-- FIN IMPLEMENTACIÓN -->>


    override suspend fun updateRoutineStatus(routineId: Long, isEnabled: Boolean) {
        Timber.d("Repository: Updating routine status for ID: $routineId to $isEnabled")
        routineDao.updateEnabledStatus(routineId, isEnabled)
        Timber.d("Repository: Routine status updated for ID: $routineId")
    }

    override suspend fun insertRoutine(routine: Routine): Long {
        Timber.d("Repository: Inserting routine: ${routine.name}")

        val routineEntity = routine.toRoutineEntity()
        val routineId = routineDao.insertRoutine(routineEntity)
        Timber.d("Repository: Routine inserted with ID: $routineId")

        // Asociar triggers y actions con el nuevo routineId
        val triggersToInsert = routine.triggers.map { it.copy(routineId = routineId) }
        triggersToInsert.forEach { triggerDao.insertTrigger(it) }

        val actionsToInsert = routine.actions.map { it.copy(routineId = routineId) }
        actionsToInsert.forEach { actionDao.insert(it) }

        Timber.d("Repository: Triggers and actions inserted for routine ID: $routineId")

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
    }

    override suspend fun deleteRoutine(routine: Routine) {
        Timber.d("Repository: Deleting routine with ID: ${routine.id} and UUID: ${routine.uuid}")
        // Llama a la función deleteRoutine del DAO, pasando la entidad RoutineEntity
        routineDao.deleteRoutine(routine.toRoutineEntity())
        Timber.d("Repository: Routine with ID: ${routine.id} deleted.")
    }



    override suspend fun getTriggersForRoutine(routineId: Long): List<Trigger> {
        // Llama a la función suspend en TriggerDao
        return triggerDao.getTriggersForRoutine(routineId)
    }


    // Implementa otras funciones de la interfaz RoutineRepository si hay más.
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

fun getTriggersForRoutine(routineId: Long): List<Trigger> {
    return TODO("Provide the return value")
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
