package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.RawValue
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "triggers",
    foreignKeys = [ForeignKey(
        entity = RoutineEntity::class,
        parentColumns = ["id"],
        childColumns = ["routineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["routineId"], name = "idx_trigger_routine_id")]
)
data class Trigger(
    val uuid: String = UUID.randomUUID().toString(),
    val routineId: Long,
    val triggerType: String,  //  Nombre mas descriptivo para el tipo de trigger
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val wifiSsid: String? = null,
    @ColumnInfo(name = "trigger_data")
    val data: @RawValue Map<String, Any> // @RawValue en la propiedad 
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readSerializable() as? LocalDateTime,
        parcel.readSerializable() as? LocalDateTime,
        parcel.readString(),
        parcel.readHashMap(HashMap::class.java.classLoader) as Map<String, Any>
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uuid)
        parcel.writeLong(routineId)
        parcel.writeString(triggerType)
        parcel.writeLong(id)
        parcel.writeSerializable(startDate)
        parcel.writeSerializable(endDate)
        parcel.writeString(wifiSsid)
        parcel.writeMap(data)
    }
    override fun describeContents(): Int = 0
}
