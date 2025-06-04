package com.example.rutinas.execution

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
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

    // region Members and Initialization

    private var routineExecutionListener: RoutineExecutionListener? = null

    fun setListener(listener: RoutineExecutionListener) {
        this.routineExecutionListener = listener
        Timber.d("RoutineExecutor: Listener set.")
    }

    companion object {
        private const val CHANNEL_ID = "RoutineNotifications"
    }

    // System Services
    private val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Audio Focus Management
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> {
                Timber.d("RoutineExecutor: AudioFocusChangeListener: Focus gained. Starting MediaPlayer if not playing.")
                mediaPlayer?.let {
                    if (!it.isPlaying) {
                        it.start()
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Timber.d("RoutineExecutor: AudioFocusChangeListener: Focus lost. Pausing MediaPlayer.")
                mediaPlayer?.pause()
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    Timber.d("RoutineExecutor: AudioFocusChangeListener: Permanent focus loss. Abandoning focus and releasing media player.")
                    abandonAudioFocus()
                    releaseMediaPlayer()
                    routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
                    repetitionScheduler?.shutdownNow()
                    repetitionScheduler = null
                    cancelAlarmNotification(ALARM_NOTIFICATION_ID)
                }
            }
        }
    }

    // Media Players and Vibrator
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Text-to-Speech
    private var textToSpeech: TextToSpeech? = null

    // Notification Reading
    private val notificationReader = NotificationReader(context)

    // Handlers and Schedulers
    private val handler = Handler(Looper.getMainLooper())
    private var repetitionScheduler: ScheduledExecutorService? = null

    // Routine State
    private var currentRoutine: Routine? = null

    // Define duration constants in milliseconds (for Alarm action)
    private val DURATION_SHORTEST_MS = 10 * 1000L
    private val DURATION_SHORT_MS = 30 * 1000L
    private val DURATION_NORMAL_MS = 60 * 1000L
    private val DURATION_LONG_MS = 120 * 1000L
    private val DURATION_LONGER_MS = 240 * 1000L
    private val PAUSE_BETWEEN_REPETITIONS_MS = 1000L

    // Notification Channel ID for the alarm notification
    private val ALARM_NOTIFICATION_CHANNEL_ID = "alarm_channel_id"
    private val ALARM_NOTIFICATION_ID = 66612

    // Coroutine scope for routine execution
    private val routineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        createNotificationChannel()
        createAlarmNotificationChannel()
        initializeTextToSpeech()
        notificationReader.initialize {}
    }

    // endregion

    // region Routine Execution Flow

    fun executeRoutine(routine: Routine, alarmId: Int) {
        Timber.d("RoutineExecutor: executeRoutine called")
        Timber.i("RoutineExecutor: Executing routine: ${routine.name} (${routine.uuid}), triggered by alarmId: $alarmId")

        cancelRoutineExecution()

        currentRoutine = routine

        routineExecutionListener?.onRoutineExecutionStart(routine.id)

        routineScope.launch {
            for (action in routine.actions) {
                Timber.d("RoutineExecutor: Attempting to execute action: ${action.actionType}")
                routineExecutionListener?.onActionStarted(action)

                runCatching {
                    executeAction(action)
                }
                    .onSuccess {
                        Timber.d("RoutineExecutor: Action ${action.actionType} finished successfully.")
                        if(action.actionType != ActionType.ALARM) {
                            routineExecutionListener?.onActionFinished(action.actionType, true)
                        }
                    }
                    .onFailure {
                        Timber.e(it, "Error executing action: ${action.actionType}")
                        routineExecutionListener?.onActionFinished(action.actionType, false)
                    }
            }
            Timber.i("RoutineExecutor: Finished executing routine: ${routine.name}")
            if(currentRoutine != null) {
                routineExecutionListener?.onRoutineExecutionFinished(currentRoutine!!.id, true)
            }
            currentRoutine = null
        }
    }

    private suspend fun executeAction(action: Action) {
        Timber.d("RoutineExecutor: executeAction called for type: ${action.actionType}")
        val actionData = action.data?.data
        when (action.actionType) {
            ActionType.ALARM -> handleAlarmAction(actionData)
            ActionType.ANNOUNCEMENT -> handleAnnouncementAction(actionData)
            ActionType.BRIGHTNESS -> handleBrightnessAction(actionData)
            ActionType.READ_NOTIFICATIONS -> handleReadNotificationsAction(actionData)
            ActionType.SOUND_MODE -> handleSoundModeAction(actionData)
            ActionType.TIME -> handleTimeAction()
            ActionType.PAUSE -> handlePauseAction(action)
            ActionType.VOLUME -> handleVolumeAction(actionData)
            else -> Timber.w("RoutineExecutor: Unknown action type: ${action.actionType}")
        }
    }

    fun cancelRoutineExecution() {
        Timber.d("RoutineExecutor: cancelRoutineExecution called.")
        stopAlarm()

        // Cancel the coroutine scope to stop the execution loop
        routineScope.cancel()
        currentRoutine?.let { routineExecutionListener?.onRoutineExecutionCancelled(it.id) }
        currentRoutine = null
        Timber.d("RoutineExecutor: Routine execution cancelled.")
    }

    // endregion

    // region Action Handlers

    private fun handleAlarmAction(data: Map<String, Any?>?) {
        Timber.d("RoutineExecutor: handleAlarmAction called with data: $data")

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

        val stopOnTap = (data?.get("stopOnTap") as? Boolean) ?: false // Corrected type to Boolean
        val onlyVibration = (data?.get("onlyVibration") as? Boolean) ?: false // Corrected type to Boolean
        val ignoreDnd = (data?.get("ignoreDnd") as? Boolean) ?: false // Corrected type to Boolean
        val durationString = data?.get("duration") as? String ?: "normal"
        val repetitionsString = data?.get("repetitions") as? String ?: "1"

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

        val repetitionsCount = repetitionsString.toIntOrNull() ?: 1
        val actionLabel = data?.get("actionLabel") as? String ?: "Alarma de Rutina"

        Timber.i("RoutineExecutor: Executing ALARM action - Label: $actionLabel, Sound URI: $alarmSound, Only Vibration: $onlyVibration, Ignore DND: $ignoreDnd, Stop on Tap: $stopOnTap, Duration: $durationString ($durationMs ms), Repetitions: $repetitionsCount")

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        var originalInterruptionFilter: Int = -1

        if (ignoreDnd && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                try {
                    originalInterruptionFilter = notificationManager.currentInterruptionFilter
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                    Timber.d("RoutineExecutor: Temporarily set interruption filter to ALARMS.")
                } catch (e: Exception) {
                    Timber.e(e, "RoutineExecutor: Error changing interruption filter for DND.")
                }
            } else {
                Timber.w("RoutineExecutor: ACCESS_NOTIFICATION_POLICY permission not granted. Cannot ignore DND.")
                displayPermissionRequiredNotification(
                    "Acceso a Políticas de Notificación",
                    "Para que la alarma ignore el modo 'No Molestar', necesitas conceder el permiso de acceso a políticas de notificación.",
                    Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                )
            }
        }

        showAlarmNotification(actionLabel)

        if (stopOnTap) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Timber.w("RoutineExecutor: Cannot enable Stop on Tap - SYSTEM_ALERT_WINDOW permission not granted.")
                displayPermissionRequiredNotification(
                    "Permiso de Superposición Necesario",
                    "Para la función 'Detener al Tocar' de la alarma, necesitas conceder el permiso 'Permiso para dibujar sobre otras aplicaciones'.",
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                )
            } else {
                routineExecutionListener?.onEnableStopOnTap()
                Timber.d("RoutineExecutor: Stop on Tap is enabled. Notifying listener.")
            }
        }

        repetitionScheduler?.shutdownNow()
        repetitionScheduler = Executors.newSingleThreadScheduledExecutor()

        val stopTask = Runnable {
            Timber.d("RoutineExecutor: Scheduled stop task triggered for a repetition cycle.")
            val player = mediaPlayer

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
                            fullAlarmCleanup(originalInterruptionFilter)
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
            val vibratorObj = vibrator

            requestAudioFocus { granted ->
                if(granted) {
                    if (!onlyVibration) {
                        player?.let {
                            try {
                                it.start()
                                Timber.d("RoutineExecutor: Alarm sound restarted for repetition after focus granted.")
                            } catch (e: Exception) {
                                Timber.e(e, "RoutineExecutor: Error starting MediaPlayer for repetition.")
                                routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
                                fullAlarmCleanup(originalInterruptionFilter)
                            }
                        } ?: Timber.w("RoutineExecutor: MediaPlayer is null, cannot restart sound for repetition.")
                    } else {
                        Timber.d("RoutineExecutor: Only vibration is true, not restarting sound.")
                    }

                    if (onlyVibration || (player != null && player.isPlaying)) {
                        vibratorObj?.let { vib ->
                            val vibrationPatternString = data?.get("vibrationPattern") as? String
                            val selectedVibrationPattern = if (!vibrationPatternString.isNullOrBlank()) {
                                try {
                                    vibrationPatternString.split(",").map { it.trim().toLong() }.toLongArray()
                                } catch (e: Exception) {
                                    Timber.e(e, "RoutineExecutor: Error parsing saved vibration pattern for repetition. Using default.")
                                    null
                                }
                            } else {
                                null
                            }

                            val patternToUse = if (selectedVibrationPattern != null && selectedVibrationPattern.isNotEmpty()) {
                                selectedVibrationPattern
                            } else {
                                longArrayOf(0, 1000, 1000)
                            }

                            val repeatIndex = if (selectedVibrationPattern != null && selectedVibrationPattern.isNotEmpty()) -1 else 0

                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val effect = VibrationEffect.createWaveform(patternToUse, repeatIndex)
                                    vib.vibrate(effect)
                                    Timber.d("RoutineExecutor: Vibration restarted (Waveform) for repetition with ${if(selectedVibrationPattern != null) "custom" else "default"} pattern.")
                                } else {
                                    @Suppress("DEPRECATION")
                                    vib.vibrate(patternToUse, repeatIndex)
                                    Timber.d("RoutineExecutor: Vibration restarted (Pattern) for repetition with ${if(selectedVibrationPattern != null) "custom" else "default"} pattern (deprecated API).")
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "RoutineExecutor: Error vibrating for repetition.")
                            }
                        } ?: Timber.w("RoutineExecutor: Vibrator not available for repetition.")
                    } else {
                        Timber.d("RoutineExecutor: Neither sound is starting nor only vibration is true for repetition. Not vibrating.")
                    }
                } else {
                    Timber.w("RoutineExecutor: Audio focus NOT granted for repetition start. Skipping sound and vibration for this cycle.")
                    routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
                }
            }
        }

        val finalCleanupTask = Runnable {
            Timber.d("RoutineExecutor: Final cleanup task triggered.")
            fullAlarmCleanup(originalInterruptionFilter)
            routineExecutionListener?.onActionFinished(ActionType.ALARM, true)

            repetitionScheduler?.shutdown()
            repetitionScheduler = null
        }

        requestAudioFocus { granted ->
            if (granted) {
                Timber.d("RoutineExecutor: Initial audio focus granted. Proceeding with first playback.")
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
                            isLooping = true
                            prepare()
                            start()
                            Timber.d("RoutineExecutor: Initial alarm sound started.")
                        } catch (e: Exception) {
                            Timber.e(e, "RoutineExecutor: Error setting up or playing initial alarm sound.")
                            routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
                            fullAlarmCleanup(originalInterruptionFilter)
                            return@apply
                        }
                    }
                } else {
                    Timber.d("RoutineExecutor: Only vibration is true, skipping initial sound playback.")
                }

                if (onlyVibration || mediaPlayer?.isPlaying == true) {
                    vibrator?.let { vib ->
                        val vibrationPatternString = data?.get("vibrationPattern") as? String
                        val selectedVibrationPattern = if (!vibrationPatternString.isNullOrBlank()) {
                            try {
                                vibrationPatternString.split(",").map { it.trim().toLong() }.toLongArray()
                            } catch (e: Exception) {
                                Timber.e(e, "RoutineExecutor: Error parsing saved vibration pattern for initial vibration. Using default.")
                                null
                            }
                        } else {
                            null
                        }

                        val patternToUse = if (selectedVibrationPattern != null && selectedVibrationPattern.isNotEmpty()) {
                            selectedVibrationPattern
                        } else {
                            longArrayOf(0, 1000, 1000)
                        }

                        val repeatIndex = if (selectedVibrationPattern != null && selectedVibrationPattern.isNotEmpty()) -1 else 0

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val effect = VibrationEffect.createWaveform(patternToUse, repeatIndex)
                                vib.vibrate(effect)
                                Timber.d("RoutineExecutor: Initial vibration started with ${if(selectedVibrationPattern != null) "custom" else "default"} pattern.")
                            } else {
                                @Suppress("DEPRECATION")
                                vib.vibrate(patternToUse, repeatIndex)
                                Timber.d("RoutineExecutor: Initial vibration started with ${if(selectedVibrationPattern != null) "custom" else "default"} pattern (deprecated API).")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "RoutineExecutor: Error during initial vibration.")
                        }
                    } ?: Timber.w("RoutineExecutor: Vibrator not available for initial vibration.")
                } else {
                    Timber.d("RoutineExecutor: Neither sound is playing nor only vibration is true. Not vibrating initially.")
                }

                val actualRepetitions = if (repetitionsCount <= 0) 1 else repetitionsCount

                Timber.d("RoutineExecutor: Scheduling first stop in ${durationMs}ms.")
                repetitionScheduler?.schedule(stopTask, durationMs, TimeUnit.MILLISECONDS)

                if (actualRepetitions > 1) {
                    for (i in 1 until actualRepetitions) {
                        val delayForNextCycleStart = i * (durationMs + PAUSE_BETWEEN_REPETITIONS_MS)
                        val delayForNextCycleStop = delayForNextCycleStart + durationMs
                        Timber.d("RoutineExecutor: Scheduling repetition ${i + 1}: Start in ${delayForNextCycleStart}ms, Stop in ${delayForNextCycleStop}ms.")

                        repetitionScheduler?.schedule(startTask, delayForNextCycleStart, TimeUnit.MILLISECONDS)
                        repetitionScheduler?.schedule(stopTask, delayForNextCycleStop, TimeUnit.MILLISECONDS)
                    }
                }

                val totalDurationIncludingPauses = (actualRepetitions - 1) * (durationMs + PAUSE_BETWEEN_REPETITIONS_MS) + durationMs
                Timber.d("RoutineExecutor: Scheduling final cleanup in ${totalDurationIncludingPauses + 100}ms (adding a small buffer).")
                repetitionScheduler?.schedule(finalCleanupTask, totalDurationIncludingPauses + 100, TimeUnit.MILLISECONDS)

            } else {
                Timber.w("RoutineExecutor: Initial audio focus NOT granted. Cannot play alarm sound or vibrate.")
                routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
                fullAlarmCleanup(originalInterruptionFilter)
            }
        }
    }

    private fun handleAnnouncementAction(data: Map<String, Any?>?) {
        Timber.d("RoutineExecutor: handleAnnouncementAction called")
        val message = data?.get("message") as? String ?: ""
        val languageCode = data?.get("languageCode") as? String // Assuming you'll add language selection later

        Timber.i("RoutineExecutor: Executing ANNOUNCEMENT action - message: \"$message\", languageCode: $languageCode")

        if (message.isNotBlank()) {
            handler.post { // Ensure TTS operations are on the main thread
                requestAudioFocusForSpeech { granted ->
                    if(granted) {
                        Timber.d("RoutineExecutor: Audio focus granted for speech. Speaking text.")
                        val locale = if (!languageCode.isNullOrBlank()) {
                            try {
                                Locale.forLanguageTag(languageCode)
                            } catch (e: Exception) {
                                Timber.e(e, "RoutineExecutor: Invalid language code: $languageCode. Using default.")
                                Locale.getDefault()
                            }
                        } else {
                            Locale.getDefault()
                        }

                        val result = textToSpeech?.setLanguage(locale)

                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Timber.e("TTS: The specified language ($locale) is not supported by the engine.")
                            displayNotification(
                                "Problema con la Voz",
                                "El idioma seleccionado para el anuncio no está soportado."
                            )
                            routineExecutionListener?.onActionFinished(ActionType.ANNOUNCEMENT, false)
                        } else {
                            textToSpeech?.speak(message, TextToSpeech.QUEUE_ADD, null, "announcementUtteranceId") // Add a Utterance ID
                        }
                    } else {
                        Timber.w("RoutineExecutor: Audio focus NOT granted for speech. Cannot speak.")
                        displayNotification(
                            "Voz No Disponible",
                            "No se pudo obtener el foco de audio para hablar."
                        )
                        routineExecutionListener?.onActionFinished(ActionType.ANNOUNCEMENT, false)
                    }
                }
            }
        } else {
            Timber.w("RoutineExecutor: Announcement message is blank. Skipping speech.")
            routineExecutionListener?.onActionFinished(ActionType.ANNOUNCEMENT, true) // Consider it finished successfully if message is empty
        }
    }

    // Add an UtteranceProgressListener to know when speech is done
    private val utteranceProgressListener = object : android.speech.tts.UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            Timber.d("TTS: Speech started for utteranceId: $utteranceId")
            // You could notify the listener here if needed
        }

        override fun onDone(utteranceId: String?) {
            Timber.d("TTS: Speech finished for utteranceId: $utteranceId")
            if (utteranceId == "announcementUtteranceId") {
                abandonAudioFocus() // Abandon audio focus after speech is done
                routineExecutionListener?.onActionFinished(ActionType.ANNOUNCEMENT, true)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            Timber.e("TTS: Speech error for utteranceId: $utteranceId")
            if (utteranceId == "announcementUtteranceId") {
                abandonAudioFocus()
                routineExecutionListener?.onActionFinished(ActionType.ANNOUNCEMENT, false)
                displayNotification(
                    "Error de Voz",
                    "Ocurrió un error al reproducir el anuncio."
                )
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            Timber.e("TTS: Speech error for utteranceId: $utteranceId with error code: $errorCode")
            if (utteranceId == "announcementUtteranceId") {
                abandonAudioFocus()
                routineExecutionListener?.onActionFinished(ActionType.ANNOUNCEMENT, false)
                displayNotification(
                    "Error de Voz",
                    "Ocurrió un error al reproducir el anuncio. Código: $errorCode"
                )
            }
        }
    }

    init {
        createNotificationChannel()
        createAlarmNotificationChannel()
        initializeTextToSpeech()
        notificationReader.initialize {}
        textToSpeech?.setOnUtteranceProgressListener(utteranceProgressListener) // Set the listener
    }


    private fun handleBrightnessAction(data: Map<String, Any?>?) {
        Timber.d("RoutineExecutor: handleBrightnessAction called")
        val brightness = (data?.get("brightness") as? Number)?.toInt() ?: 50
        val brightnessInt = (brightness / 100f * 255).toInt().coerceIn(0, 255)

        Timber.i("RoutineExecutor: Adjusting BRIGHTNESS action - brightness: $brightness% ($brightnessInt/255)")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            Timber.w("RoutineExecutor: Cannot adjust brightness - WRITE_SETTINGS permission not granted.")
            displayPermissionRequiredNotification(
                "Modificar Ajustes del Sistema",
                "Para ajustar el brillo, necesitas conceder el permiso 'Modificar ajustes del sistema' a Rutinas.",
                Settings.ACTION_MANAGE_WRITE_SETTINGS
            )
        } else {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightnessInt
                )
                Timber.d("RoutineExecutor: Brightness adjusted to $brightness%")
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

    private fun handleReadNotificationsAction(data: Map<String, Any?>?) {
        Timber.d("RoutineExecutor: handleReadNotificationsAction called")
        val excludedPackages = (data?.get("excludedPackages") as? String)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val delayMs = (data?.get("delay") as? Number)?.toInt() ?: 0

        Timber.i("RoutineExecutor: Executing READ_NOTIFICATIONS action - excluded: $excludedPackages, delay: $delayMs ms")

        if (!isNotificationListenerEnabled()) {
            Timber.w("RoutineExecutor: Notification Listener not enabled. Cannot read notifications")
            displayPermissionRequiredNotification(
                "Acceso a Notificaciones",
                "Para leer notificaciones, necesitas conceder el permiso 'Acceso a notificaciones' a Rutinas.",
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            )
            routineExecutionListener?.onActionFinished(ActionType.READ_NOTIFICATIONS, false)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                notificationManager?.activeNotifications?.let { notifications ->
                    if (notifications.isNotEmpty()) {
                        Timber.d("RoutineExecutor: Found ${notifications.size} active notifications.")
                        notificationReader.readNotifications(notifications, excludedPackages) {
                            Timber.i("RoutineExecutor: Notification reading process completed by NotificationReader.")
                            routineExecutionListener?.onActionFinished(ActionType.READ_NOTIFICATIONS, true)
                        }
                    } else {
                        Timber.i("RoutineExecutor: No active notifications found.")
                        routineExecutionListener?.onActionFinished(ActionType.READ_NOTIFICATIONS, true)
                    }
                } ?: run {
                    Timber.w("RoutineExecutor: NotificationManager is null or activeNotifications is null.")
                    displayNotification(
                        "Error Leyendo Notificaciones",
                        "No se pudo acceder al gestor de notificaciones."
                    )
                    routineExecutionListener?.onActionFinished(ActionType.READ_NOTIFICATIONS, false)
                }
            }, delayMs.toLong())
        }
    }

    private fun handleSoundModeAction(data: Map<String, Any?>?) {
        Timber.d("RoutineExecutor: handleSoundModeAction called")
        val mode = data?.get("mode") as? String ?: "normal"

        Timber.i("RoutineExecutor: Adjusting SOUND_MODE action - mode: $mode")

        try {
            when (mode) {
                "silent" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager?.isNotificationPolicyAccessGranted != true) {
                        Timber.w("RoutineExecutor: Cannot set sound mode to SILENT - ACCESS_NOTIFICATION_POLICY permission not granted.")
                        displayPermissionRequiredNotification(
                            "Acceso a Políticas de Notificación",
                            "Para cambiar el modo de sonido a 'Silencio', necesitas conceder el permiso de acceso a políticas de notificación.",
                            Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        )
                        routineExecutionListener?.onActionFinished(ActionType.SOUND_MODE, false)
                        return
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                "vibrate" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager?.isNotificationPolicyAccessGranted != true) {
                        Timber.w("RoutineExecutor: Cannot set sound mode to VIBRATE - ACCESS_NOTIFICATION_POLICY permission not granted.")
                        displayPermissionRequiredNotification(
                            "Acceso a Políticas de Notificación",
                            "Para cambiar el modo de sonido a 'Vibración', necesitas conceder el permiso de acceso a políticas de notificación.",
                            Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        )
                        routineExecutionListener?.onActionFinished(ActionType.SOUND_MODE, false)
                        return
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
                "normal" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                else -> Timber.w("RoutineExecutor: Invalid sound mode: $mode. Using normal.")
            }
            Timber.d("RoutineExecutor: Sound mode adjusted to $mode.")
            routineExecutionListener?.onActionFinished(ActionType.SOUND_MODE, true)
        } catch (e: SecurityException) {
            Timber.e("RoutineExecutor: SecurityException while adjusting sound mode: ${e.message}")
            displayNotification(
                "Error Ajustando Modo de Sonido",
                "Ocurrió un error de seguridad al intentar cambiar el modo de sonido. Asegúrate de tener los permisos necesarios."
            )
            routineExecutionListener?.onActionFinished(ActionType.SOUND_MODE, false)
        } catch (e: Exception) {
            Timber.e("RoutineExecutor: Error adjusting sound mode: ${e.message}")
            displayNotification(
                "Error Ajustando Modo de Sonido",
                "No se pudo ajustar el modo de sonido. Error: ${e.localizedMessage}"
            )
            routineExecutionListener?.onActionFinished(ActionType.SOUND_MODE, false)
        }
    }

    private fun handleTimeAction() {
        Timber.d("RoutineExecutor: handleTimeAction called")
        Timber.i("RoutineExecutor: Executing TIME action - reading current time")
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = sdf.format(calendar.time)
        val message = "Son las $formattedTime"

        handler.post {
            speakText(message)
            routineExecutionListener?.onActionFinished(ActionType.TIME, true)
        }
    }

    private fun handleVolumeAction(data: Map<String, Any?>?) {
        Timber.d("RoutineExecutor: handleVolumeAction called")
        Timber.i("RoutineExecutor: Adjusting VOLUME action - data: $data")

        var allAdjustedSuccessfully = true

        try {
            data?.forEach { (streamType, volume) ->
                val volumePercentage = (volume as? Number)?.toInt()
                if (volumePercentage != null) {
                    val stream = when (streamType) {
                        "mediaVolume" -> AudioManager.STREAM_MUSIC
                        "ringtoneVolume" -> AudioManager.STREAM_RING
                        "alarmVolume" -> AudioManager.STREAM_ALARM
                        "voiceCallVolume" -> AudioManager.STREAM_VOICE_CALL
                        "systemVolume" -> AudioManager.STREAM_SYSTEM
                        "notificationVolume" -> AudioManager.STREAM_NOTIFICATION
                        else -> {
                            Timber.w("RoutineExecutor: Unknown volume type: $streamType")
                            allAdjustedSuccessfully = false
                            null
                        }
                    }

                    stream?.let {
                        val maxVolume = audioManager.getStreamMaxVolume(it)
                        val volumeLevel = (volumePercentage / 100f * maxVolume).toInt().coerceIn(0, maxVolume)

                        audioManager.setStreamVolume(it, volumeLevel, 0)
                        Timber.d("RoutineExecutor: Set $streamType volume to $volumePercentage% (level: $volumeLevel / $maxVolume)")
                    }
                } else {
                    Timber.w("RoutineExecutor: Invalid volume percentage for $streamType: $volume")
                    allAdjustedSuccessfully = false
                }
            }
            routineExecutionListener?.onActionFinished(ActionType.VOLUME, allAdjustedSuccessfully)

        } catch (e: SecurityException) {
            Timber.e("RoutineExecutor: SecurityException while adjusting volume: ${e.message}")
            displayNotification(
                "Error Ajustando Volumen",
                "Ocurrió un error de seguridad al intentar ajustar el volumen. Asegúrate de tener los permisos necesarios."
            )
            routineExecutionListener?.onActionFinished(ActionType.VOLUME, false)
        } catch (e: Exception) {
            Timber.e("RoutineExecutor: Error adjusting volume: ${e.message}")
            displayNotification(
                "Error Ajustando Volumen",
                "No se pudo ajustar el volumen. Error: ${e.localizedMessage}"
            )
            routineExecutionListener?.onActionFinished(ActionType.VOLUME, false)
        }
    }

    private suspend fun handlePauseAction(action: Action) {
        Timber.d("RoutineExecutor: handlePauseAction called")
        val duration = action.pauseDuration
        if (duration != null && duration > 0) {
            Timber.i("RoutineExecutor: Pausing for $duration ms")
            delay(duration)
            Timber.i("RoutineExecutor: Pause completed")
            routineExecutionListener?.onActionFinished(ActionType.PAUSE, true)
        } else {
            Timber.w("RoutineExecutor: Invalid or zero pause duration specified for action: ${action.actionType}. Skipping pause.")
            routineExecutionListener?.onActionFinished(ActionType.PAUSE, false)
        }
    }

    // endregion

    // region Audio Focus Management Helper Functions

    private fun requestAudioFocus(onAudioFocusGranted: (Boolean) -> Unit) {
        Timber.d("RoutineExecutor: requestAudioFocus called.")

        val audioAttributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        } else {
            null
        }

        audioFocusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes!!)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
                .build()
        } else {
            null
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }

        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                Timber.d("RoutineExecutor: Audio focus request GRANTED.")
                onAudioFocusGranted(true)
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                Timber.d("RoutineExecutor: Audio focus request DELAYED.")
                onAudioFocusGranted(false)
            }
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Timber.w("RoutineExecutor: Audio focus request FAILED.")
                onAudioFocusGranted(false)
            }
            else -> {
                Timber.w("RoutineExecutor: Audio focus request returned unknown result: $result.")
                onAudioFocusGranted(false)
            }
        }
    }

    private fun abandonAudioFocus() {
        Timber.d("RoutineExecutor: abandonAudioFocus called.")
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }

        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                Timber.d("RoutineExecutor: Audio focus abandoned successfully.")
                audioFocusRequest = null
            }
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Timber.w("RoutineExecutor: Audio focus abandonment FAILED.")
            }
            else -> {
                Timber.w("RoutineExecutor: Audio focus abandonment returned unknown result: $result.")
            }
        }
    }

    // endregion

    // region Alarm Specific Helpers

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
            Timber.d("RoutineExecutor: MediaPlayer released.")
        }
    }

    private fun cancelAlarmNotification(notificationId: Int) {
        notificationManager?.cancel(notificationId)
        Timber.d("RoutineExecutor: Alarm notification $notificationId hidden.")
    }

    private fun fullAlarmCleanup(originalInterruptionFilter: Int) {
        Timber.d("RoutineExecutor: Performing full alarm cleanup.")

        releaseMediaPlayer()
        vibrator?.cancel()
        Timber.d("RoutineExecutor: Vibration stopped definitively.")

        if (originalInterruptionFilter != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                try {
                    notificationManager.setInterruptionFilter(originalInterruptionFilter)
                    Timber.d("RoutineExecutor: Restored original interruption filter to: $originalInterruptionFilter")
                } catch (e: Exception) {
                    Timber.e(e, "RoutineExecutor: Error restoring original interruption filter.")
                }
            } else {
                Timber.w("RoutineExecutor: Cannot restore interruption filter - ACCESS_NOTIFICATION_POLICY permission lost or not granted.")
            }
        }

        abandonAudioFocus()

        repetitionScheduler?.shutdownNow()
        repetitionScheduler = null
        Timber.d("RoutineExecutor: Repetition scheduler shut down during full cleanup.")

        cancelAlarmNotification(ALARM_NOTIFICATION_ID)

        routineExecutionListener?.onRemoveStopOnTapOverlay()
        Timber.d("RoutineExecutor: Notified listener to remove Stop on Tap overlay.")
    }

    fun stopAlarm() {
        Timber.d("RoutineExecutor: stopAlarm() called manually.")
        fullAlarmCleanup(-1)
        routineExecutionListener?.onActionFinished(ActionType.ALARM, false)
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_NOTIFICATION_CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Routine Executor Alarm notifications"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager?.createNotificationChannel(channel)
            Timber.d("RoutineExecutor: Alarm notification channel created.")
        }
    }

    private fun showAlarmNotification(labelText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val stopAlarmIntent = Intent(context, RoutineExecutionService::class.java).apply {
            action = RoutineExecutionService.ACTION_STOP_CURRENT_ALARM
            currentRoutine?.id?.let { putExtra(RoutineExecutionService.EXTRA_ROUTINE_ID, it) }
        }

        val stopAlarmPendingIntent: PendingIntent = PendingIntent.getService(
            context,
            0,
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val builder = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarma de Rutina")
            .setContentText(labelText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Detener", stopAlarmPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager.notify(ALARM_NOTIFICATION_ID, builder.build())
        Timber.d("RoutineExecutor: Alarm notification shown.")
    }

    // endregion

    // region General Utility Functions

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(context.packageName + "/" + NotificationService::class.java.name) ?: false
    }

    private fun displayNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
        Timber.d("Displayed notification: Title: \"$title\", Text: \"$text\"")
    }

    private fun displayPermissionRequiredNotification(
        permissionName: String,
        message: String,
        settingsAction: String
    ) {
        Timber.w("Permission required: $permissionName. Displaying notification to guide user.")

        val settingsIntent = Intent(settingsAction).apply {
            if (settingsAction == Settings.ACTION_MANAGE_WRITE_SETTINGS || settingsAction == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                data = Uri.parse("package:${context.packageName}")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val requestCode = settingsAction.hashCode() + permissionName.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Permiso Requerido: $permissionName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Ir a Configuración", pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

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
            notificationManager?.createNotificationChannel(channel)
            Timber.d("RoutineExecutor: General notification channel created.")
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Timber.e("TTS: The specified language is not supported")
                    displayNotification(
                        "Problema con la Voz",
                        "El idioma de texto a voz no está soportado en tu dispositivo."
                    )
                } else {
                    Timber.d("RoutineExecutor: TextToSpeech initialized successfully with default language.")
                }
            } else {
                Timber.e("TTS: TextToSpeech initialization failed with status $status")
                displayNotification(
                    "Problema con la Voz",
                    "No se pudo inicializar el motor de texto a voz."
                )
            }
        }
    }

    private fun speakText(text: String) {
        if (textToSpeech != null && textToSpeech?.engines != null && textToSpeech?.isLanguageAvailable(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE) {
            requestAudioFocusForSpeech { granted ->
                if(granted) {
                    Timber.d("RoutineExecutor: Audio focus granted for speech. Speaking text.")
                    textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
                } else {
                    Timber.w("RoutineExecutor: Audio focus NOT granted for speech. Cannot speak.")
                    displayNotification(
                        "Voz No Disponible",
                        "No se pudo obtener el foco de audio para hablar."
                    )
                }
            }

        } else {
            Timber.w("RoutineExecutor: TextToSpeech is not ready or initialized. Cannot speak.")
            displayNotification(
                "Voz No Disponible",
                "El motor de texto a voz no está listo en este momento."
            )
        }
    }

    private fun requestAudioFocusForSpeech(onAudioFocusGranted: (Boolean) -> Unit) {
        Timber.d("RoutineExecutor: requestAudioFocusForSpeech called.")

        val audioAttributes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        } else {
            null
        }

        val speechAudioFocusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes!!)
                .setWillPauseWhenDucked(false)
                .build()
        } else {
            null
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            speechAudioFocusRequest?.let { audioManager.requestAudioFocus(it) } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                Timber.d("RoutineExecutor: Audio focus request for speech GRANTED.")
                onAudioFocusGranted(true)
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                Timber.d("RoutineExecutor: Audio focus request for speech DELAYED.")
                onAudioFocusGranted(false)
            }
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Timber.w("RoutineExecutor: Audio focus request for speech FAILED.")
                onAudioFocusGranted(false)
            }
            else -> {
                Timber.w("RoutineExecutor: Audio focus request for speech returned unknown result: $result.")
                onAudioFocusGranted(false)
            }
        }
    }

    fun shutdown() {
        Timber.d("RoutineExecutor: shutdown() called.")
        stopAlarm()
        routineScope.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        notificationReader.shutdown()
        Timber.d("RoutineExecutor: RoutineExecutor fully shut down.")
    }
}
