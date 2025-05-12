package com.example.rutinas.execution

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.domain.Routine
import com.example.rutinas.receivers.AlarmStopperReceiver
import com.example.rutinas.service.NotificationService
import com.example.rutinas.utils.NotificationReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RoutineExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CHANNEL_ID = "RoutineNotifications"
    }

    private val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var textToSpeech: TextToSpeech? = null
    private val notificationReader = NotificationReader(context) // Asegúrate de que NotificationReader está importado y configurado correctamente

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper()) // Use a single handler
    // Consider using a ScheduledExecutorService for more robust repetition handling
    private var repetitionScheduler: ScheduledExecutorService? = null

    // Define duration constants in milliseconds
    private val DURATION_SHORTEST_MS = 10 * 1000L // 10 seconds
    private val DURATION_SHORT_MS = 30 * 1000L  // 30 seconds
    private val DURATION_NORMAL_MS = 60 * 1000L  // 1 minute
    private val DURATION_LONG_MS = 120 * 1000L   // 2 minutes
    private val DURATION_LONGER_MS = 240 * 1000L // 4 minutes

    // Notification Channel ID for the alarm notification
    private val ALARM_NOTIFICATION_CHANNEL_ID = "alarm_channel_id"
    private val ALARM_NOTIFICATION_ID = 12345 // A unique ID for the alarm notification

    init {
        createNotificationChannel()
        initializeTextToSpeech()
        notificationReader.initialize {} // Asegúrate de que initialize tiene un callback o se ajusta a tu implementación
    }

    fun executeRoutine(routine: Routine, alarmId: Int) {
        Timber.d("RoutineExecutor: executeRoutine called")
        Timber.i("RoutineExecutor: Executing routine: ${routine.name} (${routine.uuid}), triggered by alarmId: $alarmId")

        val handler = Handler(Looper.getMainLooper())
        CoroutineScope(Dispatchers.Main).launch {
            for (action in routine.actions) {
                runCatching { executeAction(action, handler) }
                    .onFailure { Timber.e(it, "Error executing action: $action") }
            }
            Timber.i("RoutineExecutor: Finished executing routine: ${routine.name}")
        }
    }

    private suspend fun executeAction(action: Action, handler: Handler) {
        Timber.d("RoutineExecutor: executeAction called")
        Timber.d("RoutineExecutor: Executing action: ${action.actionType}, data: ${action.data}, action id: ${action.uuid}")
        val actionType = action.actionType
        when (actionType) {
            ActionType.ALARM -> handleAlarmAction(action.data?.data)
            ActionType.ANNOUNCEMENT -> handleAnnouncementAction(action.data?.data, handler)
            ActionType.BRIGHTNESS -> handleBrightnessAction(action.data?.data)
            ActionType.READ_NOTIFICATIONS -> handleReadNotificationsAction(action.data?.data)
            ActionType.SOUND_MODE -> handleSoundModeAction(action.data?.data)
            ActionType.TIME -> handleTimeAction(handler)
            ActionType.VOLUME -> handleVolumeAction(action.data?.data)
            ActionType.PAUSE -> handlePauseAction(action)
            // ADD other action types here as you implement them
            else -> Timber.w("RoutineExecutor: Unknown action type: ${action.actionType}")
        }
    }

    private fun handleAlarmAction(data: Map<String, Any>?) {
        Timber.d("RoutineExecutor: handleAlarmAction called with data: $data")

        // Get duration preset and repetition count from data
        val durationPreset = data?.get("durationPreset") as? String ?: "normal" // Default to "normal"
        val repetitionsCount = (data?.get("repetitionsCount") as? String)?.toIntOrNull() ?: 1 // Default to 1

        // Determine duration in milliseconds based on preset
        val durationMs = when (durationPreset) {
            "más corta" -> DURATION_SHORTEST_MS
            "corta" -> DURATION_SHORT_MS
            "normal" -> DURATION_NORMAL_MS
            "larga" -> DURATION_LONG_MS
            "más larga" -> DURATION_LONGER_MS
            else -> DURATION_NORMAL_MS // Fallback to normal if preset is unknown
        }

        Timber.i("RoutineExecutor: Executing ALARM action - duration: $durationPreset ($durationMs ms), repetitions: $repetitionsCount")

        // Get Vibrator and Ringtone
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // Play Ringtone
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, alarmSound)
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = true // Loop the alarm sound
                prepare()
                start()
                Timber.d("RoutineExecutor: Alarm sound started.")
            } catch (e: Exception) {
                Timber.e(e, "RoutineExecutor: Error playing alarm sound.")
            }
        }

        // Vibrate
        vibrator?.let { vib ->
            val pattern = longArrayOf(0, 1000, 1000) // Start immediately, vibrate 1s, pause 1s
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0) // Loop the pattern
                vib.vibrate(effect)
                Timber.d("RoutineExecutor: Vibration started (Waveform).")
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0) // Loop the pattern
                Timber.d("RoutineExecutor: Vibration started (Pattern).")
            }
        } ?: Timber.w("RoutineExecutor: Vibrator not available.")


        // Show Alarm Notification
        val label = data?.get("label") as? String ?: "Alarma de Rutina"
        showAlarmNotification(label)


        // Schedule the stop logic
        repetitionScheduler?.shutdownNow() // Cancel any previous scheduler
        repetitionScheduler = Executors.newSingleThreadScheduledExecutor()

        val stopTask = Runnable {
            Timber.d("RoutineExecutor: Scheduled stop task triggered.")
            // Stop the current sound/vibration cycle but keep the scheduler alive for the next repetition
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.prepareAsync() // Prepare for the next start
                    Timber.d("RoutineExecutor: Alarm sound stopped for repetition pause.")
                }
            }
            vibrator?.cancel() // Cancel vibration
            Timber.d("RoutineExecutor: Vibration stopped for repetition pause.")

            // The next repetition will be scheduled by the scheduler itself
        }

        val startTask = Runnable {
            Timber.d("RoutineExecutor: Scheduled start task triggered for repetition.")
            // Restart sound and vibration
            mediaPlayer?.start() // Start playing again
            vibrator?.let { vib ->
                val pattern = longArrayOf(0, 1000, 1000) // Start immediately, vibrate 1s, pause 1s
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, 0) // Loop the pattern
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(pattern, 0) // Loop the pattern
                }
            }
        }

        // Define the pause duration between repetitions
        val pauseBetweenRepetitionsMs = DURATION_NORMAL_MS // Using normal duration as the pause


        // Schedule the first stop after durationMs
        repetitionScheduler?.schedule(stopTask, durationMs, TimeUnit.MILLISECONDS)

        // Schedule subsequent start and stop tasks for repetitions
        if (repetitionsCount > 1) {
            // Schedule (repetitionsCount - 1) more cycles
            for (i in 1 until repetitionsCount) {
                val nextStartTime = (durationMs + (i - 1) * (DURATION_NORMAL_MS + durationMs)) // Example: Stop, pause (e.g., 1 min), Start again
                val nextStopTime = nextStartTime + durationMs // Stop after durationMs again

                // Simple example: Stop, wait DURATION_NORMAL_MS, then start again for durationMs
                // You might want a fixed pause time instead of DURATION_NORMAL_MS
                val pauseBetweenRepetitionsMs = DURATION_NORMAL_MS // Or a different value

                repetitionScheduler?.schedule(stopTask, (durationMs + i * (durationMs + pauseBetweenRepetitionsMs)), TimeUnit.MILLISECONDS)
                repetitionScheduler?.schedule(startTask, (durationMs + i * (durationMs + pauseBetweenRepetitionsMs) + pauseBetweenRepetitionsMs), TimeUnit.MILLISECONDS)

                Timber.d("RoutineExecutor: Scheduled repetition ${i + 1}: Start in ${nextStartTime + pauseBetweenRepetitionsMs}ms, Stop in ${nextStopTime + pauseBetweenRepetitionsMs}ms.")

            }
            // Schedule the final stop and cleanup after all repetitions
            val finalStopTime = (durationMs + (repetitionsCount - 1) * (durationMs + pauseBetweenRepetitionsMs)) + durationMs
            repetitionScheduler?.schedule( {
                stopAlarm()
                Timber.d("RoutineExecutor: All repetitions completed. Final stop.")
            }, finalStopTime, TimeUnit.MILLISECONDS)


        } else {
            // If no repetitions, just schedule the final stop after the initial duration
            repetitionScheduler?.schedule( {
                stopAlarm()
                Timber.d("RoutineExecutor: Single alarm instance completed. Final stop.")
            }, durationMs, TimeUnit.MILLISECONDS)
        }


        // Important: Clean up the scheduler when the routine finishes or is cancelled
        // This needs to be handled outside of this function, perhaps in a global stopRoutine method.
    }


    // --- Helper function to show the alarm notification ---
    private fun showAlarmNotification(labelText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_NOTIFICATION_CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH // High importance for alarms
            ).apply {
                description = "Channel for Routine Executor Alarm notifications"
                setSound(null, null) // Don't play notification sound, alarm sound is separate
                enableVibration(false) // Don't vibrate notification, alarm vibration is separate
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create Intent for stopping the alarm
        val stopAlarmIntent = Intent(context, AlarmStopperReceiver::class.java).apply {
            action = AlarmStopperReceiver.ACTION_STOP_ALARM
            // Add any extras if needed to identify the alarm instance
        }

        // Create PendingIntent for the stop action
        // FLAG_IMMUTABLE is recommended for security
        val stopAlarmPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            0, // Request code, use a unique code if multiple PendingIntents are needed
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm) // Replace with your alarm icon
            .setContentTitle("Alarma de Rutina")
            .setContentText(labelText)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to be more noticeable
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Use ALARM category
            .setAutoCancel(true) // Consider if you want it to disappear when tapped (maybe not for alarm)
            .setOngoing(true) // Make the notification persistent until dismissed
            .addAction(R.drawable.ic_stop, "Detener", stopAlarmPendingIntent) // Add the stop button

        // Show the notification
        notificationManager.notify(ALARM_NOTIFICATION_ID, builder.build())
        Timber.d("RoutineExecutor: Alarm notification shown.")
    }

    // --- Method to stop the alarm ---
    fun stopAlarm() {
        Timber.d("RoutineExecutor: stopAlarm() called.")
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                Timber.d("RoutineExecutor: Alarm sound stopped.")
            }
            it.release() // Release the MediaPlayer resources
            mediaPlayer = null
        }

        vibrator?.cancel() // Cancel vibration
        Timber.d("RoutineExecutor: Vibration stopped.")

        // Cancel any pending repetition tasks and the final cleanup task
        repetitionScheduler?.shutdownNow() // This cancels all scheduled tasks immediately
        repetitionScheduler = null
        Timber.d("RoutineExecutor: Repetition scheduler shut down by manual stop.")


        // Hide the alarm notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALARM_NOTIFICATION_ID)
        Timber.d("RoutineExecutor: Alarm notification hidden by manual stop.")

        // You might want to notify the RoutineExecutionListener that the alarm action has been stopped manually
        // routineExecutionListener.onActionFinished(ActionType.ALARM, false) // Indicate not finished normally

    }
        // This depends on your overall routine execution flow.
        // routineExecutionListener.onActionFinished(ActionType.ALARM, true) // Or a specific status for being stopped

    // --- Helper method to get the Vibrator (existing, but ensure it's correct) ---
    // Make sure you have this method or the logic is directly in handleAlarmAction

    // ... existing methods like isNotificationListenerEnabled, displayPermissionRequiredNotification, etc. ...

    // Ensure you have a way to call stopAlarm() from outside RoutineExecutor if needed
    // For example, if the whole routine is cancelled.
    fun cancelCurrentAction() {
        Timber.d("RoutineExecutor: cancelCurrentAction called.")
        // If the current action is ALARM, stop it.
        // Add similar checks for other actions that might need cancellation.
        stopAlarm()
        // ... cancel other ongoing actions ...
        // You might also want to shut down the main routine executor if this cancels the whole routine
        // executorService.shutdownNow() // If you are using an executor for the whole routine
    }



    private fun handleAnnouncementAction(data: Map<String, Any>?, handler: Handler) {
        Timber.d("RoutineExecutor: handleAnnouncementAction called")
        val message = data?.get("message") as? String ?: ""

        Timber.i("RoutineExecutor: Displaying ANNOUNCEMENT action - message: \"$message\"")

        if (message.isNotBlank()) {
            // TextToSpeech permissions are generally handled by the system/TTS engine
            // No special app permissions typically required here.
            handler.post {
                speakText(message)
            }
        }
    }


    private fun handleBrightnessAction(data: Map<String, Any>?) {
        Timber.d("RoutineExecutor: handleBrightnessAction called")
        val brightness = (data?.get("brightness") as? Number)?.toInt() ?: 50
        val brightnessInt = (brightness / 100f * 255).toInt() // Convert percentage to 0-255 range

        Timber.i("RoutineExecutor: Adjusting BRIGHTNESS action - brightness: $brightness ($brightnessInt/255)")

        // Check if WRITE_SETTINGS permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            Timber.w("RoutineExecutor: Cannot adjust brightness - WRITE_SETTINGS permission not granted.")
            // IMPROVED NOTIFICATION: Inform the user about the missing permission and how to grant it
            displayPermissionRequiredNotification(
                "Modificar Ajustes del Sistema",
                "Para ajustar el brillo, necesitas conceder el permiso 'Modificar ajustes del sistema' a Rutinas.",
                Settings.ACTION_MANAGE_WRITE_SETTINGS
            )
        } else {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessInt
                )
                // Opcional: Notificación de éxito si lo deseas
                // displayNotification("Brillo Ajustado", "Brillo establecido a $brightness%")
            } catch (e: SecurityException) {
                Timber.e("RoutineExecutor: SecurityException while adjusting brightness: ${e.message}")
                displayNotification(
                    "Error Ajustando Brillo",
                    "Ocurrió un error de seguridad al intentar ajustar el brillo. Asegúrate de que el permiso 'Modificar ajustes del sistema' está concedido."
                )
            } catch (e: Exception) {
                Timber.e("RoutineExecutor: Error adjusting brightness: ${e.message}")
                displayNotification(
                    "Error Ajustando Brillo",
                    "No se pudo ajustar el brillo. Error: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun handleReadNotificationsAction(data: Map<String, Any>?) {
        Timber.d("RoutineExecutor: handleReadNotificationsAction called")
        val excludedPackages = (data?.get("excludedPackages") as? String)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val delay = (data?.get("delay") as? Number)?.toInt() ?: 0

        Timber.i("RoutineExecutor: Executing READ_NOTIFICATIONS action - excluded: $excludedPackages, delay: $delay ms")

        // Check for Notification Listener Permission
        if (!isNotificationListenerEnabled()) {
            Timber.w("RoutineExecutor: Notification Listener not enabled. Cannot read notifications")
            // IMPROVED NOTIFICATION: Inform the user about the missing permission and how to grant it
            displayPermissionRequiredNotification(
                "Acceso a Notificaciones",
                "Para leer notificaciones, necesitas conceder el permiso 'Acceso a notificaciones' a Rutinas.",
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS // Intent action to notification listener settings
            )
        } else {
            // Delay before reading notifications if specified
            Handler(Looper.getMainLooper()).postDelayed({
                notificationManager?.activeNotifications?.let { notifications ->
                    if (notifications.isNotEmpty()) {
                        Timber.d("RoutineExecutor: Found ${notifications.size} active notifications.")
                        // MODIFIED: Removed 'message ->' from the lambda
                        notificationReader.readNotifications(notifications, excludedPackages) {
                            Timber.i("RoutineExecutor: Notification reading process completed by NotificationReader.")
                            // Optional: Add any logic here that should run AFTER all notifications have been read and spoken by NotificationReader
                            // displayNotification("Lectura de Notificaciones Finalizada", "El proceso de lectura de notificaciones ha terminado.")
                        }
                    } else {
                        Timber.i("RoutineExecutor: No active notifications found.")
                        // Opcional: Notificación si no hay notificaciones activas
                        // displayNotification("Sin Notificaciones", "No hay notificaciones activas en este momento.")
                    }
                } ?: run {
                    Timber.w("RoutineExecutor: NotificationManager is null or activeNotifications is null.")
                    displayNotification(
                        "Error Leyendo Notificaciones",
                        "No se pudo acceder al gestor de notificaciones."
                    )
                }
            }, delay * 1000L.toLong()) // Convert delay to milliseconds
        }
    }

    // Reutiliza tu función existente
    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        // CORRECTED: Check if the string contains your package name and service name
        return enabledListeners?.contains(context.packageName + "/" + NotificationService::class.java.name) ?: false
    }


    private fun handleSoundModeAction(data: Map<String, Any>?) {
        Timber.d("RoutineExecutor: handleSoundModeAction called")
        val mode = data?.get("mode") as? String ?: "normal"

        Timber.i("RoutineExecutor: Adjusting SOUND_MODE action - mode: $mode")

        try {
            when (mode) {
                "silent" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                "vibrate" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                "normal" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                else -> Timber.w("RoutineExecutor: Invalid sound mode: $mode")
            }
            // Opcional: Notificación de éxito
            // displayNotification("Modo de Sonido Ajustado", "Modo de sonido cambiado a $mode")
        } catch (e: SecurityException) {
            Timber.e("RoutineExecutor: SecurityException while adjusting sound mode: ${e.message}")
            // MODIFY_AUDIO_SETTINGS is a normal permission, typically granted from Manifest.
            // If it fails due to SecurityException, it might indicate a deeper system issue or permission not granted.
            displayNotification(
                "Error Ajustando Modo de Sonido",
                "Ocurrió un error de seguridad al intentar cambiar el modo de sonido. Asegúrate de tener los permisos necesarios."
            )
        } catch (e: Exception) {
            Timber.e("RoutineExecutor: Error adjusting sound mode: ${e.message}")
            displayNotification(
                "Error Ajustando Modo de Sonido",
                "No se pudo ajustar el modo de sonido. Error: ${e.localizedMessage}"
            )
        }
    }

    private fun handleTimeAction(handler: Handler) {
        Timber.d("RoutineExecutor: handleTimeAction called")
        Timber.i("RoutineExecutor: Executing TIME action - reading current time")
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = sdf.format(calendar.time)
        val message = "Son las $formattedTime"

        // TextToSpeech permissions are generally handled by the system/TTS engine
        handler.post {
            speakText(message)
        }
    }


    private fun handleVolumeAction(data: Map<String, Any>?) {
        Timber.d("RoutineExecutor: handleVolumeAction called")
        Timber.i("RoutineExecutor: Adjusting VOLUME action - data: $data")

        try {
            data?.forEach { (streamType, volume) ->
                val volumePercentage = (volume as? Number)?.toInt()
                if (volumePercentage != null) {
                    val stream = when (streamType) {
                        "mediaVolume" -> AudioManager.STREAM_MUSIC
                        "ringtoneVolume" -> AudioManager.STREAM_RING
                        "alarmVolume" -> AudioManager.STREAM_ALARM
                        else -> {
                            Timber.w("RoutineExecutor: Unknown volume type: $streamType")
                            null
                        }
                    }

                    stream?.let {
                        val maxVolume = audioManager.getStreamMaxVolume(it)
                        // Ensure calculated volume level is within valid range [0, maxVolume]
                        val volumeLevel = (volumePercentage / 100f * maxVolume).toInt().coerceIn(0, maxVolume)
                        audioManager.setStreamVolume(it, volumeLevel, 0)
                        Timber.d("RoutineExecutor: Set $streamType volume to $volumePercentage% (level: $volumeLevel / $maxVolume)")
                        // Opcional: Notificación para cada stream ajustado
                        // displayNotification("Volumen Ajustado", "Volumen de $streamType establecido a $volumePercentage%")
                    }
                }
            }
            // Opcional: Notificación general si varios volúmenes se ajustaron
            // displayNotification("Volúmenes Ajustados", "Se ajustaron los volúmenes especificados.")
        } catch (e: SecurityException) {
            Timber.e("RoutineExecutor: SecurityException while adjusting volume: ${e.message}")
            // MODIFY_AUDIO_SETTINGS is a normal permission, typically granted from Manifest.
            displayNotification(
                "Error Ajustando Volumen",
                "Ocurrió un error de seguridad al intentar ajustar el volumen. Asegúrate de tener los permisos necesarios."
            )
        } catch (e: Exception) {
            Timber.e("RoutineExecutor: Error adjusting volume: ${e.message}")
            displayNotification(
                "Error Ajustando Volumen",
                "No se pudo ajustar el volumen. Error: ${e.localizedMessage}"
            )
        }
    }

    private suspend fun handlePauseAction(action: Action) {
        Timber.d("RoutineExecutor: handlePauseAction called")
        val duration = action.pauseDuration
        if (duration != null) {
            Timber.i("RoutineExecutor: Pausing for $duration ms")
            delay(duration)
            Timber.i("RoutineExecutor: Pause completed")
        } else {
            Timber.w("RoutineExecutor: Pause duration not specified for action: ${action.actionType}")
            // Opcional: Notificación de advertencia
            // displayNotification("Acción de Pausa Inválida", "La duración de la pausa no está especificada.")
        }
    }

    // MODIFIED: Function to display a general notification
    private fun displayNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Use your notification icon resource
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Use a unique ID for each notification, System.currentTimeMillis().toInt() is simple but might collide
        // Consider a more robust ID generation if many notifications are expected
        notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
        Timber.d("Displayed notification: Title: \"$title\", Text: \"$text\"")
    }

    // NEW: Function to display a permission required notification with an intent to settings
    private fun displayPermissionRequiredNotification(
        permissionName: String,
        message: String,
        settingsAction: String
    ) {
        Timber.w("Permission required: $permissionName. Displaying notification to guide user.")

        // Create an Intent to open the relevant settings screen
        val settingsIntent = Intent(settingsAction).apply {
            // For WRITE_SETTINGS, you need to specify the package URI
            if (settingsAction == Settings.ACTION_MANAGE_WRITE_SETTINGS) {
                data = Uri.parse("package:${context.packageName}")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Open in a new task
        }

        // Create a PendingIntent to launch the settings Intent when the user taps the notification
        val pendingIntent = PendingIntent.getActivity(
            context,
            settingsAction.hashCode(), // Use a unique request code based on action
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Use your notification icon resource
            .setContentTitle("Permiso Requerido: $permissionName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Use high priority for important permissions
            .setAutoCancel(true) // Remove notification when tapped
            .setContentIntent(pendingIntent) // Add the PendingIntent to open settings
            .addAction(0, "Ir a Configuración", pendingIntent) // Add a button to go to settings
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Allow long text to be fully displayed
            .build()

        // Use a unique ID for each permission notification
        notificationManager?.notify(settingsAction.hashCode(), notification)
        Timber.d("Displayed permission required notification for $permissionName.")
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Routine Notifications"
            val descriptionText = "Notifications for routine actions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Timber.e("TTS: The specified language is not supported")
                    // Optional: Notify user if TTS language is not supported
                    displayNotification(
                        "Problema con la Voz",
                        "El idioma de texto a voz no está soportado en tu dispositivo."
                    )
                }
            } else {
                Timber.e("TTS: TextToSpeech initialization failed with status $status")
                // Optional: Notify user if TTS initialization failed
                displayNotification(
                    "Problema con la Voz",
                    "No se pudo inicializar el motor de texto a voz."
                )
            }
        }
    }

    private fun speakText(text: String) {
        // Check if TTS is initialized and ready
        if (textToSpeech != null && textToSpeech?.engines != null && textToSpeech?.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE) {
            // Check if the device is not in silent or vibrate mode if you only want to speak in normal mode
            val ringerMode = audioManager.ringerMode
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
            } else {
                Timber.i("Skipping speech as device is in silent or vibrate mode.")
            }
        } else {
            Timber.w("TextToSpeech is not ready or initialized. Cannot speak.")
            //Notify user if TTS is not ready
            displayNotification(
            "Voz No Disponible",
            "El motor de texto a voz no está listo en este momento."
            )
        }
    }

    fun shutdown() {
        Timber.d("RoutineExecutor: shutdown() called.")
        stopAlarm() // Stop any ongoing alarm
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        notificationReader.shutdown() // Ensure NotificationReader has a shutdown method
    }
}