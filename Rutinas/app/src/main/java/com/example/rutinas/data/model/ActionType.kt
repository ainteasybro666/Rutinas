// ActionType.kt
package com.example.rutinas.data.model

enum class ActionType {
    ANNOUNCEMENT,
    ALARM,
    READ_NOTIFICATIONS, //Old NOTIFICATIONS
    TIME,
    VOLUME,
    BRIGHTNESS,
    SOUND_MODE,
    PAUSE;

    companion object {
        fun fromString(type: String): ActionType {
            return valueOf(type.uppercase().replace("NOTIFICATIONS", "READ_NOTIFICATIONS"))
        }
    }
}