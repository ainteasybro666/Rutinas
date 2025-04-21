// Action.kt
package com.example.rutinas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.UUID

@Entity(
    tableName = "actions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routineId"], name = "idx_action_routine_id")]
)
@Parcelize
data class Action(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var uuid: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val routineId: Long,
    @ColumnInfo(name = "data")
    val data: @RawValue Map<String, Any> = emptyMap(),
    val executionOrder: Int,
    val pauseDuration: Long? = null
) : Parcelable {

    fun getString(key: String): String? = data[key] as? String
    fun getInt(key: String): Int? = (data[key] as? Number)?.toInt()
    fun getBoolean(key: String): Boolean? = data[key] as? Boolean

    // Propiedades calculadas para la UI
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
            else -> "Acción Desconocida"
        }

    val description: String
        get() = when (type) {
            ActionType.ANNOUNCEMENT -> data["message"] as? String ?: "Sin mensaje"
            ActionType.ALARM -> "Alarma: ${data["time"]?.toString() ?: "No configurada"}"
            ActionType.READ_NOTIFICATIONS -> "Leer notificaciones activas"
            ActionType.TIME -> "Informar hora actual"
            ActionType.VOLUME -> {
                val mediaVolume = data["mediaVolume"] != null
                val ringtoneVolume = data["ringtoneVolume"] != null
                val alarmVolume = data["alarmVolume"] != null

                val streams = mutableListOf<String>()
                if (mediaVolume) streams.add("Media")
                if (ringtoneVolume) streams.add("Ringtone")
                if (alarmVolume) streams.add("Alarma")

                if (streams.isEmpty()) "Sin cambios en el volumen" else "Ajustar volumen: ${streams.joinToString(", ")}"
            }
            ActionType.BRIGHTNESS -> "Brillo: ${data["level"]}%"
            ActionType.SOUND_MODE -> if (data["mode"] == "silent") "Modo silencioso" else if (data["mode"] == "vibrate") "Modo vibración" else "Modo normal"
            ActionType.PAUSE -> if (data["pauseDuration"] != null) {
                val duration = (data["pauseDuration"] as? Number)?.toLong() ?: 0
                if (duration < 1000) "Pausa de $duration milisegundos" else "Pausa de ${duration.div(1000)} segundos"
            } else "Pausa"
            else -> "Unknown description"
        }

    companion object {
        fun createAction(
            routineId: Long,
            actionType: ActionType,
            data: Map<String, Any>,
            executionOrder: Int
        ): Action {        
            return Action (
                routineId = routineId,
                type = actionType,
                data = data,
                executionOrder = executionOrder
            ).apply {
                // Genera nuevo UUID automáticamente
                uuid = UUID.randomUUID().toString()
            }
        }
    }
}
