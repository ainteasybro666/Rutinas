package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import java.util.UUID

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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

    val actionType: String,

    val routineId: Long,

    @ColumnInfo(name = "data")

    val data: DataWrapper? = null,

    val executionOrder: Int,

    val pauseDuration: Long? = null
) : Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(uuid)
        parcel.writeString(actionType)
        parcel.writeLong(routineId)
        parcel.writeParcelable(data, flags)
        parcel.writeInt(executionOrder)
        parcel.writeLong(pauseDuration)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Action> {
        override fun createFromParcel(parcel: Parcel): Action {
            return Action(
                parcel.readLong(), parcel.readString() ?: UUID.randomUUID().toString(), parcel.readString() ?: "", parcel.readLong(), parcel.readParcelable(DataWrapper::class.java.classLoader), parcel.readInt(), parcel.readLong()
            )
        }
        override fun newArray(size: Int): Array<Action?> { return arrayOfNulls(size) }
    }

    fun getString(key: String): String? = data[key] as? String
    fun getInt(key: String): Int? = (data[key] as? Number)?.toInt()
    fun getBoolean(key: String): Boolean? = data[key] as? Boolean

    // Propiedades calculadas para la UI
    val title: String
        get() = when (ActionType.fromString(actionType)) {
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
        get() = when (ActionType.fromString(actionType)) {
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
            data: DataWrapper?,
            executionOrder: Int
        ): Action {
            return Action(
                routineId = routineId,
                actionType = actionType.toString(),
                data = data,
                executionOrder = executionOrder
            ).apply {
                // Genera nuevo UUID automáticamente
                uuid = UUID.randomUUID().toString()
            }
        }
    }
}
