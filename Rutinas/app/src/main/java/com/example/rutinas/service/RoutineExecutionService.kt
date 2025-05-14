package com.example.rutinas.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.routines.execution.RoutineExecutionListener
import dagger.hilt.android.AndroidEntryPoint // Import Hilt annotation
import timber.log.Timber
import javax.inject.Inject
import com.example.rutinas.data.model.RoutineEntity
import com.example.rutinas.domain.Routine
import com.example.rutinas.execution.RoutineExecutor
import com.example.rutinas.data.repository.RoutineRepository // Replace with your actual repository
import kotlinx.coroutines.* // Import coroutine stuff


@AndroidEntryPoint
class RoutineExecutionService : Service(), RoutineExecutionListener {

    @Inject
    lateinit var routineExecutor: RoutineExecutor

    @Inject
    lateinit var routineRepository: RoutineRepository // Make sure this import and injection is correct


    private val NOTIFICATION_CHANNEL_ID = "routine_execution_channel"
    private val NOTIFICATION_ID = 1

    private var stopOnTapOverlay: View? = null
    private var windowManager: WindowManager? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun onRoutineExecutionStart(routineId: Long) {
        Timber.d("RoutineExecutionService: Routine execution started for ID: $routineId")
        updateForegroundNotification("Ejecutando Rutina ID: $routineId")
    }

    override fun onActionStarted(action: Action) {
        Timber.d("RoutineExecutionService: Action started: ${action.actionType} (ID: ${action.uuid})")
        updateForegroundNotification("Ejecutando: ${action.actionType.name}")
    }

    override fun onActionFinished(actionType: ActionType, success: Boolean) {
        Timber.d("RoutineExecutionService: Action finished: $actionType, Success: $success")
    }

    override fun onRoutineExecutionFinished(routineId: Long, success: Boolean) {
        Timber.d("RoutineExecutionService: Routine execution finished for ID: $routineId, Success: $success")
        stopSelf()
    }

    override fun onRoutineExecutionCancelled(routineId: Long) {
        Timber.d("RoutineExecutionService: Routine execution cancelled for ID: $routineId")
        stopSelf()
    }


    override fun onCreate() {
        super.onCreate()
        Timber.d("RoutineExecutionService: onCreate() called.")
        routineExecutor.setListener(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager // Initialize windowManager
    }

    override fun onEnableStopOnTap() {
        Timber.d("RoutineExecutionService: onEnableStopOnTap() called. Implementing touch detection.")
        // Implement touch detection logic here
        // This involves creating a transparent overlay view and adding it to the WindowManager

        if (stopOnTapOverlay == null) {
            stopOnTapOverlay = View(this) // Create a simple view
            stopOnTapOverlay?.layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                // Use a window type that allows touches and is suitable for overlays
                // TYPE_APPLICATION_OVERLAY requires SYSTEM_ALERT_WINDOW permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                // Flags to make it non-focusable and consume touches
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or // Allows touches outside the window
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or // Notifies of touches outside
                        WindowManager.LayoutParams.FLAG_FULLSCREEN or // Optional: make it fullscreen
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, // Optional: layout within screen bounds
                PixelFormat.TRANSLUCENT // Make it transparent
            )

            // Set a touch listener on the overlay
            stopOnTapOverlay?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Timber.d("RoutineExecutionService: Touch detected on overlay. Stopping alarm.")
                    // Send the stop alarm intent to ourselves
                    val stopIntent = Intent(this, RoutineExecutionService::class.java).apply {
                        action = ACTION_STOP_CURRENT_ALARM
                    }
                    startService(stopIntent)
                    true // Consume the touch event
                } else {
                    false // Don't consume other touch events (like ACTION_UP)
                }
            }

            // Add the overlay to the window manager
            try {
                windowManager?.addView(stopOnTapOverlay, stopOnTapOverlay?.layoutParams)
                Timber.d("RoutineExecutionService: Stop on Tap overlay added to WindowManager.")
            } catch (e: Exception) {
                Timber.e(e, "RoutineExecutionService: Error adding Stop on Tap overlay.")
                // Handle the error, e.g., log a warning or notify the user that Stop on Tap failed
            }
        } else {
            Timber.d("RoutineExecutionService: Stop on Tap overlay already exists.")
        }

    }
    // You also need to remove the overlay when the alarm stops.
    // Modify your stopSelf() calls or the final cleanup logic to call a removeOverlay function.

    private fun removeStopOnTapOverlay() {
        if (stopOnTapOverlay != null) {
            try {
                windowManager?.removeView(stopOnTapOverlay)
                Timber.d("RoutineExecutionService: Stop on Tap overlay removed from WindowManager.")
            } catch (e: Exception) {
                Timber.e(e, "RoutineExecutionService: Error removing Stop on Tap overlay.")
            } finally {
                stopOnTapOverlay = null // Clear the reference
            }
        }
    }

    override fun onRemoveStopOnTapOverlay() {
        removeStopOnTapOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("RoutineExecutionService: onStartCommand() called with action: ${intent?.action}")

        startForeground(NOTIFICATION_ID, createForegroundNotification("Iniciando Servicio de Rutina..."))

        when (intent?.action) {
            ACTION_START_ROUTINE -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                if (routineId != -1L) {
                    updateForegroundNotification("Cargando Rutina ID: $routineId")
                    executeRoutine(routineId) // Start routine execution
                } else {
                    Timber.e("RoutineExecutionService: ACTION_START_ROUTINE received without routine ID extra. Stopping service.")
                    stopSelf()
                }
            }
            ACTION_STOP_CURRENT_ALARM -> {
                Timber.d("RoutineExecutionService: Received ACTION_STOP_CURRENT_ALARM")
                routineExecutor.stopAlarm()
            }
            else -> {
                Timber.w("RoutineExecutionService: Received unknown action: ${intent?.action}. Stopping service.")
                stopSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("RoutineExecutionService: onDestroy() called.")
        routineExecutor.shutdown()
        stopForeground(true)
        Timber.d("RoutineExecutionService: Foreground service stopped and notification removed.")
        removeStopOnTapOverlay() // Remove overlay on service destroy
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun executeRoutine(routineId: Long) {
        Timber.d("RoutineExecutionService: Attempting to execute routine with ID: $routineId")

        serviceScope.launch {
            val routine: Routine? = try {
                // TODO: Verify if using routineId.toString() to get the UUID is correct.
                // If routineId is the database ID (Long), you might need a different repository function
                // that fetches by ID, or you'll need to find the UUID another way.
                Timber.i("RoutineExecutionService: Fetching routine with UUID derived from ID: ${routineId.toString()}")
                // Calling the repository function
                routineRepository.getRoutineByUuid(routineId.toString())

            } catch (e: Exception) {
                Timber.e(e, "RoutineExecutionService: Error fetching routine with ID: $routineId")
                null // Return null if there's an error during fetching
            }

            if (routine != null) {
                Timber.i("RoutineExecutionService: Routine ID: ${routine.id} fetched successfully. Starting execution.")
                // Now routine is smart-casted to Routine (non-nullable) within this block
                routineExecutor.executeRoutine(routine, 0) // Use a valid alarmId if necessary
            } else {
                Timber.e("RoutineExecutionService: Could not fetch routine with ID: $routineId. Stopping service.")
                // Notify that routine execution failed
                onRoutineExecutionFinished(routineId, false) // Indicate failure
            }
        }
    }

    private fun createForegroundNotification(contentText: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Routine Execution Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for ongoing routine executions"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, com.example.rutinas.ui.main.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Rutina en Ejecución")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateForegroundNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val updatedNotification = createForegroundNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, updatedNotification)
        Timber.d("RoutineExecutionService: Foreground notification updated with text: '$contentText'")
    }

    companion object {
        const val ACTION_START_ROUTINE = "com.example.rutinas.action.START_ROUTINE"
        const val ACTION_STOP_CURRENT_ALARM = "com.example.rutinas.action.STOP_CURRENT_ALARM"
        const val EXTRA_ROUTINE_ID = "com.example.rutinas.extra.ROUTINE_ID"

        fun startRoutineExecution(context: Context, routineId: Long) {
            val startIntent = Intent(context, RoutineExecutionService::class.java).apply {
                action = ACTION_START_ROUTINE
                putExtra(EXTRA_ROUTINE_ID, routineId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }

        fun stopCurrentAlarm(context: Context) {
            val stopIntent = Intent(context, RoutineExecutionService::class.java).apply {
                action = ACTION_STOP_CURRENT_ALARM
            }
            context.startService(stopIntent)
        }
    }
}