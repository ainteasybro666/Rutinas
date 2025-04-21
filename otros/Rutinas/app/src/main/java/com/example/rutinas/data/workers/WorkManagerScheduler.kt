package com.example.rutinas.data.workers

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import android.util.Log

object WorkManagerScheduler {
    private const val TAG = "WorkManagerScheduler"

    fun scheduleAlarm(
        context: Context,
        delayInMinutes: Long,
        soundUriString: String
    ) {
        try {
            val data = workDataOf(
                AlarmWorker.KEY_SOUND_URI to soundUriString
            )

            val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
                .setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "AlarmWork_${System.currentTimeMillis()}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            Log.d(TAG, "Alarma programada para dentro de $delayInMinutes minutos")
        } catch (e: Exception) {
            Log.e(TAG, "Error programando la alarma", e)
        }
    }
}

//“delayInMinutes” se podría calcular comparando la hora actual con la hora configurada por el usuario.
//“soundUriString” podría ser la ruta de un archivo local o un recurso de tu app.