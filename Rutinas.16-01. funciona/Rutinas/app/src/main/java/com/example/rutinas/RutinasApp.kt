package com.example.rutinas

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RutinasApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // … tu código de inicialización
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}