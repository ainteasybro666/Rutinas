class MainViewModel(repository: RoutineRepository) : ViewModel() {
    val routines: Flow<List<Routine>> = repository.getAllRoutines().flowOn(Dispatchers.IO)
}