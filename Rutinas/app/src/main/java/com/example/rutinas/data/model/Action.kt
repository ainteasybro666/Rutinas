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
        get() = when (ActionType.fromString(type)) {
            ActionType.ANNOUNCEMENT -> "Anuncio"
            ActionType.ALARM -> "Alarma"
            ActionType.READ_NOTIFICATIONS -> "Leer Notificaciones"
            ActionType.TIME -> "Decir Hora"
            ActionType.VOLUME -> "Ajustar Volumen"
            ActionType.BRIGHTNESS -> "Ajustar Brillo"
            ActionType.SOUND_MODE -> "Modo de Sonido"
            ActionType.PAUSE -> "Pausa"
            else -> "Sin título"
        }

    val description: String
        get() = when (ActionType.fromString(type)) {
            ActionType.ANNOUNCEMENT -> data["message"] as? String ?: "Sin mensaje"
            ActionType.ALARM -> "Alarma: ${data["time"] ?: "No configurada"}"
            ActionType.READ_NOTIFICATIONS -> "Leer notificaciones activas"
            ActionType.TIME -> "Informar hora actual"
            ActionType.VOLUME -> {
                val volumes = mutableListOf<String>()
                if (data["mediaVolume"] != null) volumes.add("Media")
                if (data["ringtoneVolume"] != null) volumes.add("Ringtone")
                if (data["alarmVolume"] != null) volumes.add("Alarma")
                "Ajustar volumen: ${volumes.joinToString(", ")}"
            }
            ActionType.BRIGHTNESS -> "Brillo: ${data["level"]}%"
            ActionType.SOUND_MODE -> when (data["mode"]) {
                "silent" -> "Modo silencioso"
                "vibrate" -> "Modo vibración"
                else -> "Modo no especificado"
            }
            ActionType.PAUSE -> "Pausa: ${data["duration"]} segundos"
            else -> "Sin descripción"
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
