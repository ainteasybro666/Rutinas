package com.example.rutinasapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actions")
data class ActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val routineId: Int,
    val description: String,
    val orderIndex: Int         // Indica el orden en que se ejecutan
)