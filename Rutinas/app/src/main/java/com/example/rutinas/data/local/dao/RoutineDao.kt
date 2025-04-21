package com.example.rutinas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.rutinas.data.local.AppDatabase
import com.example.rutinas.data.model.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
        @Query("SELECT * FROM routines")
        fun getAllRoutines(): Flow<List<RoutineEntity>>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertRoutine(routine: RoutineEntity): Long

        @Delete
        suspend fun deleteRoutine(routine: RoutineEntity)

        @Query("SELECT * FROM routines WHERE id = :routineId")
        suspend fun getRoutineById(routineId: Long): RoutineEntity

        @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :routineId")
        suspend fun updateEnabledStatus(routineId: Long, isEnabled: Boolean)

        @Transaction
        @Query("SELECT * FROM routines")
        fun getRoutinesWithRelations(): Flow<List<AppDatabase.RoutineWithRelations>>

        @Query("SELECT * FROM routines WHERE uuid = :uuid")
        suspend fun getRoutineByUuid(uuid: String): RoutineEntity?

        @Query("SELECT id FROM routines WHERE uuid = :uuid")
        suspend fun getRoutineIdByUuid(uuid: String): Long?

        @Update
        suspend fun update(routine: RoutineEntity)
}