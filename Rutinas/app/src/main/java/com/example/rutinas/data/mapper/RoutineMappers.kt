package com.example.rutinas.data.mapper

import com.example.rutinas.domain.Routine
import com.example.rutinas.data.model.RoutineEntity
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.data.model.Action
import java.time.LocalDateTime
import com.example.rutinas.data.local.AppDatabase // Assuming AppDatabase is needed for RoutineWithRelations mapping

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

// Mapeo de RoutineWithRelations a tu modelo de dominio Routine (ideal para cargas con relaciones)
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

// Función de mapeo para Trigger (entidad) - útil si necesitas copiar/modificar antes de insertar/actualizar
// Especialmente si necesitas asegurar routineId o resetear ID para inserciones nuevas
fun Trigger.toEntity(routineId: Long): Trigger {
    return this.copy(
        id = if (this.id == 0L) 0L else this.id, // Mantener ID si existe, 0L para que Room autogenere
        routineId = routineId // <-- Asigna el ID de la rutina principal aquí
        // Otros campos se copian directamente
    )
}

// Función de mapeo para Action (entidad) - útil si necesitas copiar/modificar antes de insertar/actualizar
fun Action.toEntity(routineId: Long): Action {
    return this.copy(
        id = if (this.id == 0L) 0L else this.id, // Mantener ID si existe, 0L para que Room autogenere
        actionType = this.actionType,
        routineId = routineId, // <-- Asigna el ID de la rutina principal aquí
        data = this.data,
        executionOrder = this.executionOrder,
        pauseDuration = this.pauseDuration
    )
}

// You might also need to map Trigger and Action entities back to domain models if they were not
// entities already, but in your case, Trigger and Action seem to be used as both.
// If they were separate domain models, you'd add functions like fun TriggerEntity.toTriggerDomain(): Trigger
