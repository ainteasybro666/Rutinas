package com.example.rutinas.data.model

import android.R.attr.data
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.rutinas.ui.edit.dialogs.TriggerTypeDialog
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import timber.log.Timber
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
    val triggerType: TriggerTypeDialog.TriggerType,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val wifiSsid: String? = null,
    @ColumnInfo(name = "trigger_data")
    val data: DataWrapper
) : Parcelable {
    // Add safe getters using the DataWrapper
    fun getString(key: String): String? = data?.data?.get(key) as? String
    fun getInt(key: String): Int? =
        (data?.data?.get(key) as? Number)?.toInt() // Handles Int and Double

    fun getDouble(key: String): Double? = data?.data?.get(key) as? Double
    fun getBoolean(key: String): Boolean? = data?.data?.get(key) as? Boolean
    fun getFrequencyType(key: String): FrequencyType? =
        data?.data?.get(key) as? FrequencyType // Should work with the Gson TypeAdapter

    // Specific getter for daysOfWeek, handling list of Doubles
    fun getDaysOfWeek(key: String): List<Int>? {
        val rawList = data?.data?.get(key) as? List<*> // Get as raw List
        return rawList?.mapNotNull { item -> (item as? Number)?.toInt() } // Map each item (if Number) to Int
    }

    // Add similar getters for other specific data types if needed (e.g., LocalDateTime for Calendar trigger)
    fun getLocalDateTime(key: String): LocalDateTime? {
        // This needs specific handling based on how you save LocalDateTime in DataWrapper
        // If you save it as String, you'll need to parse it.
        // Example if saved as ISO-8601 String:
        return (data?.data?.get(key) as? String)?.let { string ->
            try {
                LocalDateTime.parse(string)
            } catch (e: Exception) {
                Timber.e("Failed to parse LocalDateTime string: $string", e)
                null
            }
        }
        // If saved as something else (like epoch seconds), the logic will differ
    }
}
