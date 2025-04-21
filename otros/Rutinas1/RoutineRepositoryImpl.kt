class RoutineRepositoryImpl(private val routineDao: RoutineDao) {
    suspend fun getAllRoutines(): Flow<List<Routine>> = routineDao.getAllRoutines()

    suspend fun saveRoutine(routine: Routine, triggers: List<Trigger>, actions: List<Action>) {
        routineDao.insertRoutine(routine)
        routineDao.insertTriggers(triggers)
        routineDao.insertActions(actions)
    }
}