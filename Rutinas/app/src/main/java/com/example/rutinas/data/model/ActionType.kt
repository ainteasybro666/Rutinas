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
        fun fromString(type: ActionType): ActionType {
            return when (type) {
                ActionType.ANNOUNCEMENT -> ActionType.ANNOUNCEMENT
                ActionType.ALARM -> ActionType.ALARM
                ActionType.READ_NOTIFICATIONS -> ActionType.READ_NOTIFICATIONS
                ActionType.TIME -> ActionType.TIME
                ActionType.VOLUME -> ActionType.VOLUME
                ActionType.BRIGHTNESS -> ActionType.BRIGHTNESS
                ActionType.SOUND_MODE -> ActionType.SOUND_MODE
                ActionType.PAUSE -> ActionType.PAUSE
            }
        }
    }
}