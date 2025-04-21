// ActionType.kt
package com.example.rutinas.data.model

enum class ActionType {
    ANNOUNCEMENT,
    ALARM,
    NOTIFICATIONS,
    TIME,
    VOLUME,
    BRIGHTNESS,
    SOUND_MODE;

    companion object {
        fun fromString(type: String): ActionType {
            return valueOf(type.uppercase())
        }
    }
}