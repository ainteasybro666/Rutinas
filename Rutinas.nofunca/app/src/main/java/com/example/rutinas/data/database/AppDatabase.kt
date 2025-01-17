package com.example.rutinas.data.database

//import androidx.constraintlayout.core.widgets.Flow
import androidx.room.*
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.rutinas.data.dao.RoutineDao
import com.example.rutinas.data.model.Routine
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [Routine::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao

    companion object {
        const val DATABASE_NAME = "rutinas_database"
    }
}
