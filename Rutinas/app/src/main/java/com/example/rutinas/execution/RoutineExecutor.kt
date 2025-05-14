package com.example.rutinas.execution

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
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
import com.example.rutinas.routines.execution.RoutineExecutionListener
import com.example.rutinas.service.NotificationService
import com.example.rutinas.service.RoutineExecutionService
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
import javax.inject.Singleton // Potentially use Singleton scope if appropriate
import kotlinx.coroutines.*

class RoutineExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var routineExecutionListener: RoutineExecutionListener? = null // Make it nullable

    // Method to set the listener AFTER the executor is constructed via Hilt
    fun setListener(listener: RoutineExecutionListener) {
        this.routineExecutionListener = listener
        Timber.d("RoutineExecutor: Listener set.")
    }

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
    private var currentRoutine: Routine? = null // Keep track of the currently executing routine

    // Define duration constants in milliseconds
    private val DURATION_SHORTEST_MS = 10 * 1000L // 10 seconds
    private val DURATION_SHORT_MS = 30 * 1000L  // 30 seconds
    private val DURATION_NORMAL_MS = 60 * 1000L  // 1 minute
    private val DURATION_LONG_MS = 120 * 1000L   // 2 minutes
    private val DURATION_LONGER_MS = 240 * 1000L // 4 minutes

    // Notification Channel ID for the alarm notification
    private val ALARM_NOTIFICATION_CHANNEL_ID = "alarm_channel_id"
    private val ALARM_NOTIFICATION_ID = 66612 // A unique ID for the alarm notification

    // Coroutine scope for routine execution
    // Use a SupervisorJob so that if one action coroutine fails, others can continue
    private val routineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())


    init {
        createNotificationChannel()
        initializeTextToSpeech()
        notificationReader.initialize {} // Asegúrate de que initialize tiene un callback o se ajusta a tu implementación
    }

    fun executeRoutine(routine: Routine, alarmId: Int) {
        Timber.d("RoutineExecutor: executeRoutine called")
        Timber.i("RoutineExecutor: Executing routine: ${routine.name} (${routine.uuid}), triggered by alarmId: $alarmId")

        currentRoutine = routine // Store the current routine

        // Notify listener that routine execution has started (check if listener is set)
        routineExecutionListener?.onRoutineExecutionStart(routine.id)


        // Use the routineScope for executing actions
        routineScope.launch {
            for (action in routine.actions) {
                Timber.d("RoutineExecutor: Attempting to execute action: ${action.actionType}")
                // Notify listener that an action is starting (check if listener is set)
                routineExecutionListener?.onActionStarted(action)

                // Execute the action
                runCatching {
                    // The actual execution logic for each action type goes here
                    // Call your existing executeAction function
                    executeAction(action, handler) // Pass the handler if needed by specific actions
                }
                    .onSuccess {
                        Timber.d("RoutineExecutor: Action ${action.actionType} finished successfully.")
                        // Notify listener that the action finished successfully (check if listener is set)
                        routineExecutionListener?.onActionFinished(action.actionType, true)
                    }
                    .onFailure {
                        Timber.e(it, "Error executing action: ${action.actionType}")
                        // Notify listener that the action failed (check if listener is set)
                        routineExecutionListener?.onActionFinished(action.actionType, false)
                        // Decide if you want to stop the whole routine on failure or continue
                        // For now, let's continue with the next action
                    }
            }
            Timber.i("RoutineExecutor: Finished executing routine: ${routine.name}")
            // Notify listener that routine execution has finished (check if listener is set)
            routineExecutionListener?.onRoutineExecutionFinished(routine.id, true) // Assuming success if all actions attempted
            currentRoutine = null // Clear current routine
        }
    }

    // Your existing suspend function for executing actions
    private suspend fun executeAction(action: Action, handler: Handler) {
        Timber.d("RoutineExecutor: executeAction called for type: ${action.actionType}")
        // Access data safely using action.data?.data if your DataWrapper requires it
        val actionData = action.data?.data
        when (action.actionType) {
            ActionType.ALARM -> handleAlarmAction(actionData)
            ActionType.ANNOUNCEMENT -> handleAnnouncementAction(actionData, handler)
            ActionType.BRIGHTNESS -> handleBrightnessAction(actionData)
            ActionType.READ_NOTIFICATIONS -> handleReadNotificationsAction(actionData)
            ActionType.SOUND_MODE -> handleSoundModeAction(actionData)
            // Note: If handleTimeAction doesn't need data, keep the call as you had it
            ActionType.TIME -> handleTimeAction(handler)
            ActionType.PAUSE -> handlePauseAction(action) // Pass the whole action if needed
            // ADD other action types here as you implement them
            else -> Timber.w("RoutineExecutor: Unknown action type: ${action.actionType}")
        }
    }

    private fun handleAlarmAction(data: Map<String, Any>?) {
        Timber.d("RoutineExecutor: handleAlarmAction called with data: $data")

        // --- Get data from the DataWrapper ---
        // Get Alarm Sound URI
        val alarmSoundUriString = data?.get("alarmSoundUri") as? String
        val alarmSound: Uri = if (!alarmSoundUriString.isNullOrBlank()) {
            try {
                Uri.parse(alarmSoundUriString)
            } catch (e: Exception) {
                Timber.e(e, "RoutineExecutor: Error parsing saved alarm sound URI. Using default.")
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        val stopOnTap = (data?.get("stopOnTap") as? String).toBoolean()

        // Get Only Vibration state
        val onlyVibration = (data?.get("onlyVibration") as? String).toBoolean()

        // Get Ignore DND state
        val ignoreDnd = (data?.get("ignoreDnd") as? String).toBoolean()

        // Get Duration (assuming it's a String preset or custom value)
        val durationString = data?.get("duration") as? String ?: "normal" // Default to "normal"

        // Determine duration in milliseconds based on the saved value
        val durationMs = when (durationString) {
            "más corta" -> DURATION_SHORTEST_MS
            "corta" -> DURATION_SHORT_MS
            "normal" -> DURATION_NORMAL_MS
            "larga" -> DURATION_LONG_MS
            "más larga" -> DURATION_LONGER_MS
            else -> {
                Timber.w("RoutineExecutor: Unknown duration preset or invalid custom duration: $durationString. Using normal.")
                DURATION_NORMAL_MS
            }
        }

        // Get Repetitions (assuming it's a String preset or custom value)
        val repetitionsString = data?.get("repetitions") as? String ?: "1" // Default to "1"
        val repetitionsCount = repetitionsString.toIntOrNull() ?: 1 // Default to 1 if parsing fails

        // Get Action Label
        val actionLabel = data?.get("actionLabel") as? String ?: "Alarma de Rutina"
        // --- End of getting data ---

        Timber.i("RoutineExecutor: Executing ALARM action - Label: $actionLabel, Sound URI: $alarmSound, Only Vibration: $onlyVibration, Ignore DND: $ignoreDnd, Stop on Tap: $stopOnTap, Duration: $durationString ($durationMs ms), Repetitions: $repetitionsCount")

        // Get Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // --- Handle Ignore DND ---
        var originalNotificationPolicyAccess: Boolean = false // Flag to indicate if we modified the policy
        var originalInterruptionFilter: Int = -1 // To store the original DND filter

        if (ignoreDnd && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                try {
                    originalInterruptionFilter = notificationManager.currentInterruptionFilter // Save original state
                    // Set filter to allow alarms (this is a basic approach)
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    originalNotificationPolicyAccess = true // Indicate that we modified the policy
                    Timber.d("RoutineExecutor: Temporarily set interruption filter to ALARMS.")
                } catch (e: Exception) {
                    Timber.e(e, "RoutineExecutor: Error changing interruption filter for DND.")
                    // Continue execution, but without ignoring DND
                }
            } else {
                Timber.w("RoutineExecutor: ACCESS_NOTIFICATION_POLICY permission not granted. Cannot ignore DND.")
            }
        }

        // --- Play Ringtone (if not only vibration) ---
        if (!onlyVibration) {
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(context, alarmSound)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                    }
                    isLooping = true // Loop the alarm sound
                    prepare()
                    start()
                    Timber.d("RoutineExecutor: Alarm sound started.")
                } catch (e: Exception) {
                    Timber.e(e, "RoutineExecutor: Error playing alarm sound.")
                    // Handle error: Should a sound failure stop the whole action?
                    // For now, we log and continue, allowing vibration (if applicable) to proceed.
                    // You might want to notify the listener of a partial failure or the whole action failure.
                    // routineExecutionListener?.onActionFinished(ActionType.ALARM, false) // Example of notifying failure
                }
            }
        } else {
            Timber.d("RoutineExecutor: Only vibration is true, not playing sound.")
        }


        // --- Vibrate ---
        // Always vibrate if onlyVibration is true, or if sound is playing (default vibration)
        if (onlyVibration || mediaPlayer?.isPlaying == true) {
            vibrator?.let { vib ->
                // Using a default vibration pattern if onlyVibration is true or if sound is playing
                val patternToUse = longArrayOf(0, 1000, 1000) // Start immediately, vibrate 1s, pause 1s

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(patternToUse, 0) // Loop the pattern
                    vib.vibrate(effect)
                    Timber.d("RoutineExecutor: Vibration started (Waveform) with default pattern.")
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(patternToUse, 0) // Loop the pattern
                    Timber.d("RoutineExecutor: Vibration started (Pattern) with default pattern.")
                }
            } ?: Timber.w("RoutineExecutor: Vibrator not available.")
        } else {
            Timber.d("RoutineExecutor: Neither sound is playing nor only vibration is true. Not vibrating.")
        }


        // Show Alarm Notification
        showAlarmNotification(actionLabel) // Use the saved action label

        // ** Handle Stop on Tap based on state and permission **
        if (stopOnTap) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Timber.w("RoutineExecutor: Cannot enable Stop on Tap - SYSTEM_ALERT_WINDOW permission not granted.")
                // Inform the user about the missing permission and how to grant it
                displayPermissionRequiredNotification(
                    "Permiso de Superposición Necesario",
                    "Para la función 'Detener al Tocar' de la alarma, necesitas conceder el permiso 'Permiso para dibujar sobre otras aplicaciones'.",
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION // Intent action to overlay settings
                )
                // You might want to inform the listener that Stop on Tap could NOT be enabled
                // routineExecutionListener?.onStopOnTapEnableFailed() // Optional: Add this method to listener
            } else {
                // Permission is granted or not needed (API < M), proceed to enable Stop on Tap
                routineExecutionListener?.onEnableStopOnTap()
                Timber.d("RoutineExecutor: Stop on Tap is enabled. Notifying listener.")
            }
        }

        // Schedule the stop logic using ScheduledExecutorService
        repetitionScheduler?.shutdownNow() // Cancel any previous scheduler
        repetitionScheduler = Executors.newSingleThreadScheduledExecutor()

        // Within handleAlarmAction, define the runnables like this:

        val stopTask = Runnable {
            Timber.d("RoutineExecutor: Scheduled stop task triggered for a repetition cycle.")
            val player = mediaPlayer // Use a local variable for clarity

            if (player != null && player.isPlaying) {
                try {
                    player.stop()
                    if (repetitionScheduler != null && !repetitionScheduler!!.isShutdown) {
                        try {
                            player.prepareAsync()
                            Timber.d("RoutineExecutor: MediaPlayer prepared for next start.")
                        } catch (e: Exception) {
                            Timber.e(e, "RoutineExecutor: Error preparing MediaPlayer for next start.")
                            routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
                            repetitionScheduler?.shutdownNow()
                            repetitionScheduler = null
                        }
                    } else {
                        player.release()
                        mediaPlayer = null
                        Timber.d("RoutineExecutor: MediaPlayer released as no more repetitions planned.")
                    }
                    Timber.d("RoutineExecutor: Alarm sound stopped for repetition pause.")
                } catch (e: Exception) {
                    Timber.e(e, "RoutineExecutor: Error stopping MediaPlayer.")
                }
            } else {
                Timber.d("RoutineExecutor: MediaPlayer not playing, no need to stop.")
            }

            vibrator?.cancel()
            Timber.d("RoutineExecutor: Vibration stopped for repetition pause.")
        }

        val startTask = Runnable {
            Timber.d("RoutineExecutor: Scheduled start task triggered for repetition.")
            val player = mediaPlayer
            val vibratorObj = vibrator // Use a local variable

            if (!onlyVibration) {
                player?.let {
                    try {
                        it.start()
                        Timber.d("RoutineExecutor: Alarm sound restarted for repetition.")
                    } catch (e: Exception) {
                        Timber.e(e, "RoutineExecutor: Error starting MediaPlayer for repetition.")
                        routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
                        repetitionScheduler?.shutdownNow()
                        repetitionScheduler = null
                    }
                } ?: Timber.w("RoutineExecutor: MediaPlayer is null, cannot restart sound for repetition.")
            } else {
                Timber.d("RoutineExecutor: Only vibration is true, not restarting sound.")
            }

            if (onlyVibration || (player != null && player.isPlaying)) {
                vibratorObj?.let { vib ->
                    val patternToUse = longArrayOf(0, 1000, 1000)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = VibrationEffect.createWaveform(patternToUse, 0)
                            vib.vibrate(effect)
                            Timber.d("RoutineExecutor: Vibration restarted (Waveform) for repetition with default pattern.")
                        } else {
                            @Suppress("DEPRECATION")
                            vib.vibrate(patternToUse, 0)
                            Timber.d("RoutineExecutor: Vibration restarted (Pattern) for repetition with default pattern.")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "RoutineExecutor: Error vibrating for repetition.")
                    }
                } ?: Timber.w("RoutineExecutor: Vibrator not available for repetition.")
            } else {
                Timber.d("RoutineExecutor: Neither sound is starting nor only vibration is true for repetition. Not vibrating.")
            }
        }

        // Define the pause duration between repetitions
        val pauseBetweenRepetitionsMs = DURATION_NORMAL_MS

        // Schedule the first stop after durationMs
        Timber.d("RoutineExecutor: Scheduling initial stop in ${durationMs}ms.")
        repetitionScheduler?.schedule(stopTask, durationMs, TimeUnit.MILLISECONDS)


        // Schedule subsequent cycles for repetitions
        if (repetitionsCount > 1) {
            for (i in 1 until repetitionsCount) {
                val delayForNextCycleStart = durationMs + (i - 1) * (durationMs + pauseBetweenRepetitionsMs) + pauseBetweenRepetitionsMs
                val delayForNextCycleStop = delayForNextCycleStart + durationMs
                Timber.d("RoutineExecutor: Scheduling repetition ${i + 1}: Start in ${delayForNextCycleStart}ms, Stop in ${delayForNextCycleStop}ms.")

                repetitionScheduler?.schedule(startTask, delayForNextCycleStart, TimeUnit.MILLISECONDS)
                repetitionScheduler?.schedule(stopTask, delayForNextCycleStop, TimeUnit.MILLISECONDS)
            }
        }

        // Schedule the final cleanup task (hide notification, release resources)
        val totalDurationIncludingPauses = durationMs + (repetitionsCount - 1) * (durationMs + pauseBetweenRepetitionsMs)
        Timber.d("RoutineExecutor: Scheduling final cleanup in ${totalDurationIncludingPauses}ms.")
        repetitionScheduler?.schedule( {
            Timber.d("RoutineExecutor: Final cleanup task triggered.")
            // Hide the alarm notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(ALARM_NOTIFICATION_ID)
            Timber.d("RoutineExecutor: Alarm notification hidden.")

            // Release MediaPlayer and stop Vibrator definitively
            mediaPlayer?.release()
            mediaPlayer = null
            vibrator?.cancel()
            Timber.d("RoutineExecutor: Alarm fully stopped and resources released after all repetitions.")

            // Restore original DND settings if they were modified
            if (ignoreDnd && originalNotificationPolicyAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                try {
                    // Restore the original interruption filter
                    if (originalInterruptionFilter != -1) {
                        notificationManager?.setInterruptionFilter(originalInterruptionFilter)
                        Timber.d("RoutineExecutor: Restored original interruption filter to: $originalInterruptionFilter")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "RoutineExecutor: Error restoring original interruption filter.")
                }
            }

            // Notify listener that the ALARM action is finished (successfully after repetitions)
            routineExecutionListener?.onActionFinished(ActionType.ALARM, true) // Use safe call

            // Shutdown the scheduler after the final task
            repetitionScheduler?.shutdown() // Use shutdown() as all tasks are scheduled
            repetitionScheduler = null

        }, totalDurationIncludingPauses, TimeUnit.MILLISECONDS)
    }



    // --- Helper function to show the alarm notification ---
    private fun showAlarmNotification(labelText: String) {
        // ... (existing channel creation code) ...
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

        // Create Intent for stopping the alarm - NOW SENDING TO RoutineExecutionService
        val stopAlarmIntent = Intent(context, RoutineExecutionService::class.java).apply {
            action = RoutineExecutionService.ACTION_STOP_CURRENT_ALARM
            // Add any extras if needed to identify the alarm instance or routine ID
            // For now, stopping the alarm will stop the currently active ALARM action in the service
            currentRoutine?.id?.let { putExtra(RoutineExecutionService.EXTRA_ROUTINE_ID, it) }
        }

        // Create PendingIntent for the stop action
        // FLAG_IMMUTABLE is recommended for security
        val stopAlarmPendingIntent: PendingIntent = PendingIntent.getService( // Use getService
            context,
            0, // Request code, use a unique code if multiple PendingIntents are needed
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Build the notification
        val builder = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm) // Replace with your alarm icon (ensure you have this)
            .setContentTitle("Alarma de Rutina")
            .setContentText(labelText)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority to be more noticeable
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Use ALARM category
            // .setAutoCancel(true) // Consider if you want it to disappear when tapped (maybe not for alarm)
            .setOngoing(true) // Make the notification persistent until dismissed
            .addAction(R.drawable.ic_stop, "Detener", stopAlarmPendingIntent) // Add the stop button (ensure you have ic_stop)

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

        // ** Remove the Stop on Tap overlay **
        routineExecutionListener?.onRemoveStopOnTapOverlay()
        Timber.d("RoutineExecutor: Notified listener to remove Stop on Tap overlay.")

        // Notify the RoutineExecutionListener that the alarm action has been stopped manually
        routineExecutionListener?.onActionFinished(ActionType.ALARM, false) // Use safe call

    }

    // This method is likely redundant now, as stopAlarm() and cancelRoutineExecution() cover cancellation.
    // You can remove this if its functionality is covered by the other methods.
    // If you need a method to stop *only* the currently executing action (and not the whole routine),
    // you would need more complex state management in RoutineExecutor to know which action is active
    // and how to specifically cancel it. For now, let's assume stopAlarm() handles the ALARM case,
    // and cancelRoutineExecution() handles stopping everything.
    /*
    fun cancelCurrentAction() {
        Timber.d("RoutineExecutor: cancelCurrentAction called.")
        // If the current action is ALARM, stop it.
        stopAlarm()
        // ... add logic to cancel other specific action types if necessary ...
        // Example: If a PAUSE action is active, you would need to cancel the delay coroutine.
        // This requires keeping a reference to the coroutine for the active action.
    }
    */


    // Add a method to call when the entire routine is cancelled
    // Note: This should also be called by the RoutineExecutionService if it's stopped prematurely
    fun cancelRoutineExecution() {
        Timber.d("RoutineExecutor: cancelRoutineExecution called.")
        stopAlarm() // Stop any ongoing alarm action
        // TODO: Add logic to stop other ongoing actions if any - This is complex and might require
        // keeping track of the Coroutine Jobs for each action if they are long-running or suspendable.

        // Cancel the coroutine scope to stop the execution loop
        routineScope.cancel()
        currentRoutine?.let { routineExecutionListener?.onRoutineExecutionCancelled(it.id) } // Use safe call
        currentRoutine = null
        Timber.d("RoutineExecutor: Routine execution cancelled.")
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
        routineScope.cancel() // Cancel the scope
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        notificationReader.shutdown() // Ensure NotificationReader has a shutdown method
    }
}