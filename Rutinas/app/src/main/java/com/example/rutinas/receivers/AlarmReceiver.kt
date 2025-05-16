package com.example.rutinas.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import com.example.rutinas.execution.RoutineExecutor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var routineRepository: RoutineRepository
    @Inject lateinit var routineExecutor: RoutineExecutor
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val uuid = intent.getStringExtra(EXTRA_ROUTINE_UUID) ?: return
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        Timber.d("AlarmReceiver: Received alarm id=$alarmId, uuid=$uuid")

        CoroutineScope(Dispatchers.IO).launch {
            val routine = routineRepository.getRoutineByUuid(uuid)
            if (routine != null) {
                if (routine.isEnabled) {
                    Timber.i("AlarmReceiver: Routine ${routine.name} (${routine.uuid}) is enabled. Executing.")
                    routineExecutor.executeRoutine(routine, alarmId)

                    // Find the specific trigger that fired
                    val firedTrigger = routine.triggers.find {
                        // Assuming generateAlarmId logic is consistent
                        "${routine.uuid}-${it.uuid}".hashCode() == alarmId
                    }

                    firedTrigger?.let { trigger ->
                        // Reprogram the trigger if it's a recurring TIME trigger
                        if (trigger.triggerType == "TIME") {
                            val frequency = trigger.data?.data?.get("frequency") as? String
                            if (frequency == "daily" || frequency == "weekly" || frequency == "monthly") {
                                Timber.d("Reprogramando trigger recurrente: ${trigger.uuid}")
                                // Cancel the specific trigger before rescheduling
                                val triggerAlarmId = "${routine.uuid}-${trigger.uuid}".hashCode()
                                val pendingIntent = alarmScheduler.createPendingIntent(routine, triggerAlarmId)
                                alarmScheduler.alarmManager.cancel(pendingIntent)
                                // Schedule the next occurrence of this specific trigger
                                alarmScheduler.scheduleTimeTrigger(routine, trigger)
                            } else {
                                Timber.d("Trigger de tiempo único o sin frecuencia especificada. No se reprograma.")
                            }
                        } else {
                            Timber.d("Trigger no es de tiempo. No se reprograma automáticamente.")
                        }
                    }


                } else {
                    Timber.i("AlarmReceiver: Routine ${routine.name} (${routine.uuid}) is disabled. Skipping execution.")
                    // Cancel all alarms for this disabled routine
                    alarmScheduler.cancel(routine)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Rutina '${routine.name}' está deshabilitada", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Timber.w("Routine no encontrada para uuid=$uuid")
                // Corrected: Provide a placeholder name for the dummy Routine
                // Attempt to cancel the specific alarm even if the routine is not found
                val dummyRoutine = Routine(uuid = uuid, name = "Unknown Routine")
                val pendingIntent = alarmScheduler.createPendingIntent(dummyRoutine, alarmId)
                alarmScheduler.alarmManager.cancel(pendingIntent)

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Rutina desconocida (UUID: $uuid) disparada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "com.example.rutinas.extra.ALARM_ID"
        const val EXTRA_ROUTINE_UUID = "com.example.rutinas.extra.ROUTINE_UUID"
    }
}
