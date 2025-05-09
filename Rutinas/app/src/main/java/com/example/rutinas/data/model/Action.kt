package com.example.rutinas.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
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
data class Action(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var uuid: String = UUID.randomUUID().toString(),
    val actionType: ActionType,
    val routineId: Long,
    @ColumnInfo(name = "data")
    val data: DataWrapper? = null,
    var executionOrder: Int,
    val pauseDuration: Long? = null
) : Parcelable {
    fun getString(key: String): String? = data?.data?.get(key) as? String
    fun getInt(key: String): Int? = (data?.data?.get(key) as? Number)?.toInt()
    fun getBoolean(key: String): Boolean? = data?.data?.get(key) as? Boolean

    // Propiedades calculadas para la UI
    val title: String
        get() = when (actionType) {
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
        get() = when (actionType) {
            ActionType.ANNOUNCEMENT -> getString("message") ?: "Sin mensaje"
            ActionType.ALARM -> "Alarma: ${getString("time") ?: "No configurada"}"
            ActionType.READ_NOTIFICATIONS -> "Leer notificaciones activas"
            ActionType.TIME -> "Informar hora actual"
            ActionType.VOLUME -> {
                val volumes = mutableListOf<String>()
                if (getInt("mediaVolume") != null) volumes.add("Media")
                if (getInt("ringtoneVolume") != null) volumes.add("Ringtone")
                if (getInt("alarmVolume") != null) volumes.add("Alarma")
                "Ajustar volumen: ${volumes.joinToString(", ")}"
            }
            ActionType.BRIGHTNESS -> "Brillo: ${getInt("level")}%"
            ActionType.SOUND_MODE -> when (getString("mode")) {
                "silent" -> "Modo silencioso"
                "vibrate" -> "Modo vibración"
                else -> "Modo no especificado"
            }
            ActionType.PAUSE -> "Pausa: ${getInt("duration")} segundos"
        }

    companion object {
        fun createAction(
            routineId: Long,
            actionType: ActionType,
            data: DataWrapper?,
            executionOrder: Int
        ): Action {
            return Action(
                routineId = routineId,
                actionType = actionType,
                data = data,
                executionOrder = executionOrder
            )
        }
    }
}
