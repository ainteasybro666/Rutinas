package com.example.rutinas.util

object TimeUtils {
    fun calculateDelayInMinutes(targetTimeMillis: Long): Long {
        val currentTimeMillis = System.currentTimeMillis()
        return ((targetTimeMillis - currentTimeMillis) / (1000 * 60))
            .coerceAtLeast(0) // Asegura que el delay no sea negativo
    }
}