package com.example.rutinas.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.rutinas.data.model.Action

@Dao
interface ActionDao {
    @Insert
    suspend fun insert(action: Action)

    @Query("SELECT * FROM actions WHERE routineId = :routineId")
    suspend fun getActionsForRoutine(routineId: Long): List<Action>

    @Query("SELECT * FROM actions WHERE uuid = :uuid")
    suspend fun getActionByUuid(uuid: String): Action?

    @Query("UPDATE actions SET executionOrder = :order WHERE uuid = :uuid")
    suspend fun updateActionOrder(uuid: String, order: Int)

    @Query("DELETE FROM actions WHERE routineId = :routineId")
    suspend fun deleteActionsForRoutine(routineId: Long)

    @Update
    suspend fun update(action: Action)

    @Delete
    suspend fun delete(action: Action)
}
