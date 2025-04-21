package RutinasApp;

@HiltAndroidApp
class RutinasApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configurar logging solo en desarrollo
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}