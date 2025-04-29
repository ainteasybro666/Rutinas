package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.time.LocalDateTime
import java.util.UUID

@Parcelize
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
    val triggerType: String,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val wifiSsid: String? = null,
    @ColumnInfo(name = "trigger_data")
    val data: DataWrapper
) : Parcelable