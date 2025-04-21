abstract class BaseViewModel : ViewModel() {
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    protected fun handleError(error: Throwable) {
        _errorMessage.postValue(error.message ?: "Error desconocido")
        Timber.e(error)
    }
}