package com.example.rutinas.data.local

import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.rutinas.data.local.dao.ActionDao
import com.example.rutinas.data.local.dao.RoutineDao
import com.example.rutinas.data.local.dao.TriggerDao
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.local.DataWrapperTypeConverter
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.data.model.RoutineEntity

@Database(
    entities = [RoutineEntity::class, Trigger::class, Action::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(
    DataWrapperTypeConverter::class,
    FrequencyTypeConverter::class,
    LocalDateTimeConverter::class
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun triggerDao(): TriggerDao
    abstract fun actionDao(): ActionDao

    data class RoutineWithRelations(
        @Embedded val routine: RoutineEntity,
        @Relation(
            parentColumn = "id",
            entityColumn = "routineId",
            entity = Trigger::class
        )
        val triggers: List<Trigger>,
        @Relation(
            parentColumn = "id",
            entityColumn = "routineId",
            entity = Action::class
        )
        val actions: List<Action>
    )

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE routines ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE triggers ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE actions ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")

                database.execSQL("UPDATE routines SET uuid = hex(randomblob(16))")
                database.execSQL("UPDATE triggers SET uuid = hex(randomblob(16))")
                database.execSQL("UPDATE actions SET uuid = hex(randomblob(16))")
            }
        }
    }
}
