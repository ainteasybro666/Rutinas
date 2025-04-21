package com.example.rutinasapp.data

import androidx.room.*

@Dao
interface RoutineDao {

    // ────────── Rutina ──────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    // ────────── Triggers ──────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTriggers(triggers: List<TriggerEntity>)

    @Delete
    suspend fun deleteTriggers(triggers: List<TriggerEntity>)

    // ────────── Actions ──────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<ActionEntity>)

    @Delete
    suspend fun deleteActions(actions: List<ActionEntity>)

    // ──────────────────────────────
    // Consultas con relaciones
    // ──────────────────────────────
    @Transaction
    @Query("SELECT * FROM routines")
    suspend fun getAllRoutinesWithRelations(): List<RoutineWithRelations>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineWithRelations(routineId: Int): RoutineWithRelations
}