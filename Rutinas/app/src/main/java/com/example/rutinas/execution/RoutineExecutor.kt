package com.example.rutinas.execution

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.domain.Routine
import com.example.rutinas.util.NotificationReader // Assuming this exists
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RoutineExecutor(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "RoutineNotifications"
    }

    private val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var textToSpeech: TextToSpeech? = null
    private val notificationReader = NotificationReader(context)


    init {
        createNotificationChannel()
        initializeTextToSpeech()
        notificationReader.initialize {} // Assuming initialize doesn't need a callback
    }


    fun executeRoutine(routine: Routine, alarmId: Int) {
        Timber.i("RoutineExecutor: Executing routine: ${routine.name} (${routine.uuid}), triggered by alarmId: $alarmId")

        val handler = Handler(Looper.getMainLooper()) // For TTS

        routine.actions.forEach { action ->
            Timber.d("RoutineExecutor: Executing action: ${action.type}, data: ${action.data}, action id: ${action.uuid}")
            when (action.type) {
                "ALARM" -> handleAlarmAction(action.data)
                "ANNOUNCEMENT" -> handleAnnouncementAction(action.data, handler)
                "BRIGHTNESS" -> handleBrightnessAction(action.data)
                "READ_NOTIFICATIONS" -> handleReadNotificationsAction(action.data)
                "SOUND_MODE" -> handleSoundModeAction(action.data)
                "TIME" -> handleTimeAction(handler)
                "VOLUME" -> handleVolumeAction(action.data)
                "PAUSE" -> handlePauseAction(action.data)
                else -> Timber.w("RoutineExecutor: Unknown action type: ${action.type}")
            }
        }

        Timber.i("RoutineExecutor: Finished executing routine: ${routine.name}")
    }

    private fun handleAlarmAction(data: Map<String, Any>?) {
        val duration = (data?.get("duration") as? Number)?.toInt() ?: 30
        val repeatEnabled = data?.get("repeatEnabled") as? Boolean ?: false

        Timber.i("RoutineExecutor: Triggering ALARM action - duration: $duration, repeat: $repeatEnabled")
        // TODO: Implement alarm logic here.  This might involve:
        //  - Playing a sound
        //  - Vibrating the device
        //  - Showing a notification
        //  - Potentially scheduling a repeating alarm if repeatEnabled is true
        // You'll likely need to use the AudioManager, Vibrator, and potentially the AlarmManager.
        // Example (requires permissions in manifest):
        //  val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        //  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //      vibrator.vibrate(VibrationEffect.createOneShot(duration * 1000L, VibrationEffect.DEFAULT_AMPLITUDE))
        //  } else {
        //      vibrator.vibrate(duration * 1000L)
        //  }
    }


    private fun handleAnnouncementAction(data: Map<String, Any>?, handler: Handler) {
        val message = data?.get("message") as? String ?: ""
        // val volume = (data?.get("volume") as? Number)?.toInt() ?: 50  // Unused for now

        Timber.i("RoutineExecutor: Displaying ANNOUNCEMENT action - message: \"$message\"")

        if (message.isNotBlank()) {
            handler.post {
                speakText(message)
            }
        }
        //  TODO: Decide how to use the volume parameter (if needed).
        //  It's not clear from the fragment how this volume is supposed to be applied.
    }


    private fun handleBrightnessAction(data: Map<String, Any>?) {
        val brightness = (data?.get("brightness") as? Number)?.toInt() ?: 50
        val brightnessFloat = brightness / 100f

        Timber.i("RoutineExecutor: Adjusting BRIGHTNESS action - brightness: $brightness ($brightnessFloat)")

        try {
            // Check if WRITE_SETTINGS permission is granted
            if (Settings.System.canWrite(context)) {
                // Set system brightness (requires WRITE_SETTINGS permission)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (brightnessFloat * 255).toInt() // Convert percentage to 0-255 range
                )
            } else {
                Timber.w("RoutineExecutor: Cannot adjust brightness - WRITE_SETTINGS permission not granted.")
                displayNotification(
                    mapOf(
                        "title" to "Permission Required",
                        "text" to "Rutinas needs permission to change brightness."
                    )
                )
            }
        } catch (e: SecurityException) {
            Timber.e("RoutineExecutor: SecurityException while adjusting brightness: ${e.message}")
            displayNotification(
                mapOf(
                    "title" to "Permission Error",
                    "text" to "Rutinas encountered an error while changing brightness."
                )
            )
        } catch (e: Exception) {
            Timber.e("RoutineExecutor: Error adjusting brightness: ${e.message}")
        }
    }

    private fun handleReadNotificationsAction(data: Map<String, Any>?) {
        val excludedPackages = (data?.get("excludedPackages") as? String)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val delay = (data?.get("delay") as? Number)?.toInt() ?: 0

        Timber.i("RoutineExecutor: Executing READ_NOTIFICATIONS action - excluded: $excludedPackages, delay: $delay")

        // Check for Notification Listener Permission
        if (isNotificationListenerEnabled()) {
            Handler(Looper.getMainLooper()).postDelayed({
                notificationManager?.activeNotifications?.let { notifications ->
                    notificationReader.readNotifications(notifications, excludedPackages) {}
                }
            }, delay * 1000L)
        } else {
            Timber.w("RoutineExecutor: Notification Listener not enabled. Cannot read notifications")
            displayNotification(
                mapOf(
                    "title" to "Permission Required",
                    "text" to "Rutinas needs Notification Access to read notifications"
                )
            )
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName + "/" + NotificationReader::class.java.name) ?: false
    }



    private fun handleSoundModeAction(data: Map<String, Any>?) {
        val mode = data?.get("mode") as? String ?: "normal"

        Timber.i("RoutineExecutor: Adjusting SOUND_MODE action - mode: $mode")

        try {
            when (mode) {
                "silent" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                "vibrate" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                "normal" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                else -> Timber.w("RoutineExecutor: Invalid sound mode: $mode")
            }
        } catch (e: SecurityException) {
            Timber.e("RoutineExecutor: SecurityException while adjusting sound mode: ${e.message}")
            displayNotification(
                mapOf(
                    "title" to "Permission Required",
                    "text" to "Rutinas needs permission to change sound mode."
                )
            )
        } catch (e: Exception) {
            Timber.e("RoutineExecutor: Error adjusting sound mode: ${e.message}")
        }
    }

    private fun handleTimeAction(handler: Handler) {
        Timber.i("RoutineExecutor: Executing TIME action - reading current time")
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault()) // Format: 3:20 PM
        val formattedTime = sdf.format(calendar.time)
        val message = "Son las $formattedTime"

        handler.post {
            speakText(message)
        }
    }


    private fun handleVolumeAction(data: Map<String, Any>?) {
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
                        val volumeLevel = (volumePercentage / 100f * maxVolume).toInt()
                        audioManager.setStreamVolume(it, volumeLevel, 0)  // Flags = 0 (no UI)
                        Timber.d("RoutineExecutor: Set $streamType volume to $volumePercentage% (level: $volumeLevel / $maxVolume)")
                    }
                }
            }
        } catch (e: SecurityException) {
            Timber.e("RoutineExecutor: SecurityException while adjusting volume: ${e.message}")
            displayNotification(
                mapOf(
                    "title" to "Permission Required",
                    "text" to "Rutinas needs permission to change volume settings."
                )
            )
        } catch (e: Exception) {
            Timber.e("RoutineExecutor: Error adjusting volume: ${e.message}")
        }
    }

    private fun handlePauseAction(data: Map<String, Any>?) {
        val duration = (data?.get("duration") as? Number)?.toLong() ?: 0L
        val unit = data?.get("unit") as? String ?: "seconds"

        val delayMillis = when (unit) {
            "milliseconds" -> duration
            "seconds" -> duration * 1000
            "minutes" -> duration * 60 * 1000
            else -> {
                Timber.w("RoutineExecutor: Invalid pause unit: $unit. Defaulting to seconds.")
                duration * 1000
            }
        }

        Timber.i("RoutineExecutor: Executing PAUSE action - duration: $duration $unit (delay: $delayMillis ms)")

        try {
            Thread.sleep(delayMillis)
        } catch (e: InterruptedException) {
            Timber.e("RoutineExecutor: Pause interrupted: ${e.message}")
            Thread.currentThread().interrupt()
        }
    }


    private fun displayNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Routine Notification"
        val text = data["text"] ?: "A routine action was triggered."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
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
                }
            } else {
                Timber.e("TTS: TextToSpeech initialization failed")
            }
        }
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        notificationReader.shutdown()
    }
}