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
    entities = [Routine::class], // Lista de entidades (tablas)
    version = 1,                // Versión de la base de datos
    exportSchema = false        // No exportar el esquema
)
@TypeConverters(Converters::class) // Si necesitas TypeConverters
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao // Vincula el DAO con la base de datos

    companion object {
        const val DATABASE_NAME = "app_database" // Nombre de la base de datos
    }
}
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val trigger: String,
    val frequency: String,
    val dayType: String,
    val dayValue: String,
    val notify5MinBefore: Boolean,
    val isActive: Boolean,
    val actions: List<String> // Puedes usar un TypeConverter para listas
)

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<Routine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine)

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)
}

