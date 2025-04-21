package com.example.rutinas.data.model

data class ActionUi(
    val id: Long,
    val uuid: String,
    val type: ActionType,
    val routineId: Long,
    val data: String,
    val executionOrder: Int,
    val pauseDuration: Long?,
) {

    val title: String
        get() = when (type) {
            ActionType.ANNOUNCEMENT -> "Anuncio"
            ActionType.ALARM -> "Alarma"
            ActionType.READ_NOTIFICATIONS -> "Leer Notificaciones"
            ActionType.TIME -> "Decir Hora"
            ActionType.VOLUME -> "Ajustar Volumen"
            ActionType.BRIGHTNESS -> "Ajustar Brillo"
            ActionType.SOUND_MODE -> "Modo de Sonido"
            ActionType.PAUSE -> "Pausa"
        }

    val description: String
        get() = when (type) {
            ActionType.ANNOUNCEMENT -> data.substringAfter("\"message\":", "Sin mensaje").substringBefore("}", "Sin mensaje").replace("\"", "")
            ActionType.ALARM -> "Alarma: ${data.substringAfter("\"time\":", "No configurada").substringBefore("}", "No configurada").replace("\"", "")}"
            ActionType.READ_NOTIFICATIONS -> "Leer notificaciones activas"
            ActionType.TIME ->  "Informar hora actual"
            ActionType.VOLUME -> {
                val volumes = mutableListOf<String>()
                if (data.contains("\"mediaVolume\"")) volumes.add("Media")
                if (data.contains("\"ringtoneVolume\"")) volumes.add("Ringtone")
                if (data.contains("\"alarmVolume\"")) volumes.add("Alarma")
                "Ajustar volumen: ${volumes.joinToString(", ")}"
            }
            ActionType.BRIGHTNESS -> "Brillo: ${data.substringAfter("\"level\":", "0").substringBefore("}", "0").replace("\"", "")}%"
            ActionType.SOUND_MODE -> when {
                data.contains("\"mode\":\"silent\"") -> "Modo silencioso"
                data.contains("\"mode\":\"vibrate\"") -> "Modo vibración"
                else -> "Modo no especificado"
            }
            ActionType.PAUSE -> if (pauseDuration != null) {
                if (pauseDuration < 1000) "Pausa de $pauseDuration milisegundos"
                else "Pausa de ${pauseDuration.div(1000)} segundos"
            } else {
                "Pausa"
            }
       }
}
