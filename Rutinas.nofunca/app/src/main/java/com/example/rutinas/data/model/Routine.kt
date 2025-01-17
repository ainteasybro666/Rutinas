package com.example.rutinas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String = "",

// Información del trigger.
// Ej: triggerType = "TIME", triggerData = "08:00"
    val triggerType: String = "",
    val triggerData: String = "",

// Información de la acción principal (opcional, si la usas).
    val actionType: String = "",
    val actionData: String = "",

// Campos extra que tu Compose está usando
// (“Once”, “Daily”, “Weekly”, “Monthly”)
    val frequency: String = "Once",

// “None”, “Weekday”, “Date”
    val dayType: String = "None",

// “Monday” o “15”
    val dayValue: String = "",

// true/false
    val notify5MinBefore: Boolean = false,
    val isActive: Boolean = true,

// Si quieres guardar la lista de acciones en Room,
// necesitas escribir un TypeConverter que transforme
// la lista a JSON y viceversa. Dejo un campo “actions”
// con valor por defecto vacío.
    val actions: List<String> = emptyList(),

    val createdAt: Long = System.currentTimeMillis()
)
