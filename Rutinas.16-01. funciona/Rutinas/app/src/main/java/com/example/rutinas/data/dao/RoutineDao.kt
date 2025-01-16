package com.example.rutinas.data.dao

import androidx.room.*
import com.example.rutinas.data.model.Routine
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<Routine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("SELECT * FROM routines WHERE isActive = 1")
    fun getActiveRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun getRoutineById(id: Long): Flow<Routine?>
}