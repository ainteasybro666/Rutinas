package com.example.rutinas.domain

import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.Trigger
import java.time.LocalDateTime

data class Routine(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val isEnabled: Boolean = true,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val triggers: List<Trigger> = emptyList(),
    val actions: List<Action> = emptyList()
)