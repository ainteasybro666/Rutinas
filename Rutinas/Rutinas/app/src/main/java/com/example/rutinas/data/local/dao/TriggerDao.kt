package com.example.rutinas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.rutinas.data.model.Trigger

@Dao
interface TriggerDao {
    @Insert
    suspend fun insertTrigger(trigger: Trigger)

    @Query("SELECT * FROM triggers WHERE routineId = :routineId")
    suspend fun getTriggersForRoutine(routineId: Long): List<Trigger>

    @Insert
    suspend fun insert(trigger: Trigger)

    @Update
    suspend fun update(trigger: Trigger)

    @Delete
    suspend fun delete(trigger: Trigger)
}
