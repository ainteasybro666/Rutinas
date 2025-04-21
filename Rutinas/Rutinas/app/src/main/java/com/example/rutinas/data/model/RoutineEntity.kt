package com.example.rutinas.data.model

import android.os.Parcelable
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.rutinas.domain.Routine
import kotlinx.coroutines.flow.Flow
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "routines")
@Parcelize
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val isEnabled: Boolean = true,
    val createdDate: LocalDateTime = LocalDateTime.now()
) : Parcelable