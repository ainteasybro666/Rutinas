package com.example.rutinas.data.model

enum class ActionType {
    ANNOUNCEMENT,
    ALARM,
    READ_NOTIFICATIONS,
    TIME,
    VOLUME,
    BRIGHTNESS,
    SOUND_MODE,
    PAUSE;

    companion object {
        fun fromString(value: String): ActionType {
            return when (value) {
                "ANNOUNCEMENT" -> ANNOUNCEMENT
                "ALARM" -> ALARM
                "READ_NOTIFICATIONS" -> READ_NOTIFICATIONS
                "TIME" -> TIME
                "VOLUME" -> VOLUME
                "BRIGHTNESS" -> BRIGHTNESS
                "SOUND_MODE" -> SOUND_MODE
                "PAUSE" -> PAUSE
                else -> throw IllegalArgumentException("Invalid ActionType value: $value")
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            ANNOUNCEMENT -> "ANNOUNCEMENT"
            ALARM -> "ALARM"
            READ_NOTIFICATIONS -> "READ_NOTIFICATIONS"
            TIME -> "TIME"
            VOLUME -> "VOLUME"
            BRIGHTNESS -> "BRIGHTNESS"
            SOUND_MODE -> "SOUND_MODE"
            PAUSE -> "PAUSE"
        }
    }
}
