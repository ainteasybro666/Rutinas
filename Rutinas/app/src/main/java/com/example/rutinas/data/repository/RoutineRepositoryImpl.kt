package com.example.rutinas.data.repository

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
    private val triggerDao: TriggerDao
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
    }

    override suspend fun insertRoutine(routine: Routine): Long {
        Timber.d("Repository: Inserting routine: ${routine.name}")

        // Mapea el modelo de dominio Routine a RoutineEntity para insertar la entidad principal
        val routineId = routineDao.insertRoutine(routine.toRoutineEntity())
        Timber.d("Repository: Routine inserted with ID: $routineId")

        // Inserta los triggers asociados a la nueva rutina
        // Asegúrate de asignar el routineId recién generado a los triggers antes de insertarlos
        val triggersToInsert = routine.triggers.map { it.copy(routineId = routineId) }
        triggersToInsert.forEach { triggerDao.insertTrigger(it) } // Usa el método insertTrigger del DAO

        // Inserta las acciones asociadas a la nueva rutina
        // Asegúrate de asignar el routineId recién generado a las acciones antes de insertarlas
        val actionsToInsert = routine.actions.map { it.copy(routineId = routineId) }
        actionsToInsert.forEach { actionDao.insert(it) } // Usa el método insert del DAO

        Timber.d("Repository: Triggers and actions inserted for routine ID: $routineId")
        return routineId // Devuelve el ID de la rutina insertada
    }

    override suspend fun updateActions(actions: List<Action>) {
        Timber.d("Repository: Updating a list of actions (${actions.size}) individually")
        // Itera sobre la lista y actualiza cada Action usando el mé-to-do update del DAO
        actions.forEach { action ->
            actionDao.update(action)
        }
        Timber.d("Repository: List of actions updated individually")
    }


    override suspend fun getRoutineByUuid(uuid: String): Routine? {
        Timber.d("Repository: Getting routine by UUID: $uuid")
        // Obtiene la rutina por UUID, si existe
        val routineEntity = routineDao.getRoutineByUuid(uuid)

        // Si se encontró la rutina, carga sus triggers y actions por routineId
        return routineEntity?.let { entity ->
            val triggers = triggerDao.getTriggersForRoutine(entity.id)
            val actions = actionDao.getActionsForRoutine(entity.id)

            // Mapea RoutineEntity, triggers y actions a tu modelo de dominio Routine
            entity.toRoutineDomain(triggers, actions)
        } ?: run {
            Timber.d("Repository: Routine with UUID: $uuid not found")
            null // Si la rutina no se encontró, devuelve null
        }
    }

    override suspend fun updateActionsOrder(actions: List<Action>) {
        Timber.d("Repository: Updating actions order for ${actions.size} actions")
        // Itera sobre la lista y actualiza el orden de cada acción usando el DAO
        actions.forEach { action ->
            actionDao.updateActionOrder(action.uuid, action.executionOrder)
        }
        Timber.d("Repository: Actions order updated")
    }

    override suspend fun updateAction(action: Action) {
        Timber.d("Repository: Updating action with UUID: ${action.uuid}")
        actionDao.update(action) // Usa el método update del ActionDao para una sola acción
        Timber.d("Repository: Action updated with UUID: ${action.uuid}")
    }

    override suspend fun deleteAction(action: Action) {
        Timber.d("Repository: Deleting action with UUID: ${action.uuid}")
        actionDao.delete(action) // Usa el método delete del ActionDao para una sola acción
        Timber.d("Repository: Action deleted with UUID: ${action.uuid}")
    }

    // Implementación de updateRoutine
    override suspend fun updateRoutine(routine: Routine) {
        Timber.d("Repository: Updating routine: ${routine.name} with ID: ${routine.id}")

        // Actualiza la entidad Routine principal
        routineDao.update(routine.toRoutineEntity())
        Timber.d("Repository: Main routine entity updated")

        // --- Lógica de actualización de Triggers y Actions (Eliminar y Re-insertar) ---
        // 1. Elimina todos los triggers y acciones existentes asociados a esta rutina
        //    Usando los métodos delete...ForRoutine que añadimos a los DAOs.
        triggerDao.deleteTriggersForRoutine(routine.id)
        actionDao.deleteActionsForRoutine(routine.id)
        Timber.d("Repository: Deleted old triggers and actions for routine ID: ${routine.id}")


        // 2. Inserta los triggers y acciones actualizados desde el modelo de dominio Routine
        //    Asegúrate de que tienen el routineId correcto (ya lo tienen en el modelo Routine)
        routine.triggers.forEach { triggerDao.insertTrigger(it) }
        routine.actions.forEach { actionDao.insert(it) }
        Timber.d("Repository: Inserted updated triggers and actions for routine ID: ${routine.id}")

        Timber.d("Repository: Routine, triggers, and actions updated for ID: ${routine.id}")
    }

    // NEW: Implement the deleteRoutine function
    override suspend fun deleteRoutine(routine: Routine) {
        Timber.d("Repository: Deleting routine with ID: ${routine.id} and UUID: ${routine.uuid}")

        // 1. Delete associated triggers
        Timber.d("Repository: Deleting triggers for routine ID: ${routine.id}")
        triggerDao.deleteTriggersForRoutine(routine.id) // Assuming you have this DAO method

        // 2. Delete associated actions
        Timber.d("Repository: Deleting actions for routine ID: ${routine.id}")
        actionDao.deleteActionsForRoutine(routine.id) // Assuming you have this DAO method

        // 3. Delete the routine itself
        Timber.d("Repository: Deleting the routine entity for ID: ${routine.id}")
        routineDao.deleteRoutine(routine.toRoutineEntity()) // Assuming you have a delete method in RoutineDao

        Timber.d("Repository: Routine and associated data deleted successfully for ID: ${routine.id}")
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