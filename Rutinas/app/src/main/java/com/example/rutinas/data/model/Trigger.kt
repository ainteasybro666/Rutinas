package com.example.rutinas.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.rutinas.data.model.RoutineEntity
import com.example.rutinas.data.local.Converters
import kotlinx.parcelize.Parcelize
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
@Parcelize
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
) : Parcelable
