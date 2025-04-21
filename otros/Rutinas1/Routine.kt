@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val isEnabled: Boolean = false
)

@Entity(tableName = "triggers")
data class Trigger(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val type: String,
    @ColumnInfo(name = "trigger_data") val data: String  // JSON
)

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<Routine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine)
}