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
import android.widget.Toast
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
import com.example.rutinas.receivers.AlarmReceiver
import kotlinx.coroutines.* // Import coroutine stuff


@AndroidEntryPoint
class RoutineExecutionService : Service(), RoutineExecutionListener {

    @Inject
    lateinit var routineExecutor: RoutineExecutor

    @Inject
    lateinit var routineRepository: RoutineRepository // Make sure this import and injection is correct

    @Inject
    lateinit var alarmScheduler: com.example.rutinas.alarms.AlarmScheduler

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
            // ACTION_START_ROUTINE ya no es la acción que inicia el servicio desde el Receiver
            // El Receiver inicia el servicio sin una acción específica, pero con extras
            null -> { // Si no hay acción, asumimos que es una alarma disparada por el Receiver
                val routineUuid = intent?.getStringExtra(AlarmReceiver.EXTRA_ROUTINE_UUID)
                val alarmId = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)

                if (routineUuid != null && alarmId != -1) {
                    alarmId?.let { id -> // Si alarmId no es nulo, ejecuta este bloque con 'id' como Int
                        Timber.d("RoutineExecutionService: Received alarm intent from Receiver. UUID: $routineUuid, Alarm ID: $id")
                        updateForegroundNotification("Cargando Rutina...")
                        // Ejecutar la lógica principal de la alarma
                        handleAlarmTriggered(routineUuid, id) // 'id' es un Int no nulo
                    } ?: run {
                        // Este bloque se ejecuta si alarmId es nulo (aunque con -1 por defecto es poco probable)
                        Timber.e("AlarmReceiver.EXTRA_ALARM_ID was unexpectedly null")
                        stopSelf() // O alguna otra acción de error
                    }
                } else {
                    Timber.e("RoutineExecutionService: Required extras missing from alarm intent. UUID: $routineUuid, Alarm ID: $alarmId. Stopping service.")
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

    // <<-- NUEVA FUNCIÓN PARA MANEJAR LA LÓGICA DEL RECEIVER -->>
    private fun handleAlarmTriggered(routineUuid: String, alarmId: Int) {
        Timber.i("RoutineExecutionService: Handling triggered alarm for UUID: $routineUuid, Alarm ID: $alarmId")
        serviceScope.launch {
            val routine = try {
                // Usar el repositorio para obtener la rutina por UUID
                routineRepository.getRoutineByUuid(routineUuid)
            } catch (e: Exception) {
                Timber.e(e, "RoutineExecutionService: Error fetching routine with UUID: $routineUuid")
                null
            }

            // <<-- ESTE BLOQUE AHORA ESTÁ DENTRO DE LA CORRUTINA serviceScope.launch -->>
            if (routine != null) {
                if (routine.isEnabled) {
                    Timber.i("RoutineExecutionService: Routine ${routine.name} (${routine.uuid}) is enabled. Executing.")
                    updateForegroundNotification("Ejecutando: ${routine.name}")
                    routineExecutor.executeRoutine(routine, alarmId) // Ejecutar la rutina

                    // <<-- LÓGICA DE REPROGRAMACIÓN (MIGRADA DESDE EL RECEIVER) -->>
                    val firedTrigger = routine.triggers.find {
                        "${routine.uuid}-${it.uuid}".hashCode() == alarmId // Asegúrate de que la lógica de ID coincida
                    }

                    firedTrigger?.let { trigger ->
                        if (trigger.triggerType == "TIME") {
                            val frequency = trigger.data?.data?.get("frequency") as? String
                            if (frequency == "daily" || frequency == "weekly" || frequency == "monthly") {
                                Timber.d("Reprogramming recurring trigger: ${trigger.uuid}")
                                // Cancel the specific trigger before rescheduling
                                // Llama a una función pública en AlarmScheduler para cancelar una alarma específica
                                alarmScheduler.cancelSingleAlarm(routine.uuid, trigger.uuid)
                                // Schedule the next occurrence of this specific trigger
                                alarmScheduler.scheduleTimeTrigger(routine, trigger) // Asegúrate de que scheduleTimeTrigger esté disponible públicamente en AlarmScheduler
                            } else {
                                Timber.d("Time trigger is not recurring or frequency not specified. Not reprogramming.")
                            }
                        } else {
                            Timber.d("Trigger is not a time trigger. Not reprogramming automatically.")
                        }
                    }
                    // <<-- FIN LÓGICA DE REPROGRAMACIÓN -->>

                } else {
                    Timber.i("RoutineExecutionService: Routine ${routine.name} (${routine.uuid}) is disabled. Skipping execution.")
                    // Cancel all alarms for this disabled routine
                    // Llama a una función pública en AlarmScheduler para cancelar todas las alarmas de la rutina
                    alarmScheduler.cancelRoutineAlarms(routine) // Asegúrate de que cancelRoutineAlarms esté disponible públicamente en AlarmScheduler
                    // Optionally show a Toast on the main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Rutina '${routine.name}' está deshabilitada", Toast.LENGTH_SHORT).show()
                    }
                }
            } else { // <<-- ESTE ELSE CORRESPONDE AL if (routine != null)
                Timber.w("RoutineExecutionService: Routine not found for uuid=$routineUuid")
                // Attempt to cancel the specific alarm even if the routine is not found
                // Llama a una función pública para cancelar una alarma específica por UUID y Alarm ID
                alarmScheduler.cancelSingleAlarmByInfo(routineUuid, alarmId) // Necesitas implementar esta función en AlarmScheduler

                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Rutina desconocida (UUID: $routineUuid) disparada", Toast.LENGTH_SHORT).show()
                }
            }
            // <<-- stopSelf() AHORA TAMBIÉN ESTÁ DENTRO DE LA CORRUTINA -->>
            // Stop the service after handling the alarm
            stopSelf()
        } // <<-- Aquí termina la corrutina
    } // <<-- Aquí termina handleAlarmTriggered
// <<-- FIN NUEVA FUNCIÓN -->>

    override fun onBind(intent: Intent?): IBinder? { // <-- onBind está en su lugar correcto
        return null
    }

// Esta función ya no se llama desde onStartCommand para iniciar la ejecución de una alarma recibida
    
    private fun executeRoutine(routineId: Long) {
        Timber.d("RoutineExecutionService: Attempting to execute routine with ID: $routineId")

        serviceScope.launch {
            val routine: Routine? = try {

                // Calling the repository function
                // Usa getRoutineById si EXTRA_ROUTINE_ID realmente es el ID de la base de datos
                routineRepository.getRoutineById(routineId) // Asumiendo que esta función existe y funciona

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