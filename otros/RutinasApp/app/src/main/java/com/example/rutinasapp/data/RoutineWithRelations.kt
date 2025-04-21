package com.example.rutinasapp.data

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineWithRelations(
    @Embedded
    val routine: RoutineEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val triggers: List<TriggerEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val actions: List<ActionEntity>
)