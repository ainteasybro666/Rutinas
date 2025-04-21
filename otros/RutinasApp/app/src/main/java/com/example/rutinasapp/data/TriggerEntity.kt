package com.example.rutinasapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "triggers")
data class TriggerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val routineId: Int,
    val triggerType: String,    // Ejemplo: "TIME", "CALENDAR", etc.
    val timeInMillis: Long?     // Solo para triggers de tiempo (puedes ampliarlo)
)