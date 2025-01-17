package com.example.rutinas.data.workers

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AlarmWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AlarmWorker"
        private const val SOUND_DURATION_MS = 5000L
        const val KEY_SOUND_URI = "SOUND_URI"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando AlarmWorker")

            val soundUriString = inputData.getString(KEY_SOUND_URI)
            if (soundUriString.isNullOrEmpty()) {
                Log.e(TAG, "URI de sonido no proporcionada")
                return@withContext Result.failure()
            }

            val soundUri = Uri.parse(soundUriString)
            var mediaPlayer: MediaPlayer? = null
            try {
                mediaPlayer = MediaPlayer.create(applicationContext, soundUri)
                if (mediaPlayer == null) {
                    Log.e(TAG, "No se pudo crear MediaPlayer para: $soundUri")
                    return@withContext Result.failure()
                }

                mediaPlayer.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error en MediaPlayer: what=$what, extra=$extra")
                    true
                }

                Log.d(TAG, "Reproduciendo sonido: $soundUri")
                mediaPlayer.start()
                delay(SOUND_DURATION_MS)

                mediaPlayer.stop()
                Log.d(TAG, "Reproducción completada")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error durante la reproducción", e)
                Result.failure()
            } finally {
                try {
                    mediaPlayer?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error liberando MediaPlayer", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error general en AlarmWorker", e)
            Result.failure()
        }
    }
}