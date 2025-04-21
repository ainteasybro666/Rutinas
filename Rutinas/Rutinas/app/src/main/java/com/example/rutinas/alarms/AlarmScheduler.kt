package com.example.rutinas.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.rutinas.data.model.RoutineEntity
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.domain.Routine
import com.example.rutinas.receivers.AlarmReceiver
import timber.log.Timber
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(routine: Routine) {
        Timber.d("Programando alarmas para la rutina: ${routine.uuid}, ${routine.name}")

        // Cancelar alarmas existentes antes de programar nuevas
        cancel(routine)

        routine.triggers.filter { it.triggerType == "TIME" }.forEach { trigger ->
            scheduleTimeTrigger(routine, trigger)
        }
    }

    private fun scheduleTimeTrigger(routine: Routine, trigger: Trigger) {
        if (trigger.hour == null || trigger.minute == null) {
            throw IllegalArgumentException("Hour and minute must be specified for time triggers.")
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, trigger.hour)
            set(Calendar.MINUTE, trigger.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Adjust for past times (set for next occurrence)
            val frequency = trigger.data["frequency"] as? String ?: "daily"
            when (frequency) {
                "daily" -> {
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                "weekly" -> {
                    if (trigger.dayOfWeek == null || trigger.dayOfWeek!!.isEmpty()) {
                        // Skip scheduling and log a warning
                        Timber.w("No days of the week specified for weekly trigger. Skipping scheduling: ${routine.name}")
                        return
                    } else {
                        if (timeInMillis <= System.currentTimeMillis()) {
                            do {
                                add(Calendar.DAY_OF_WEEK, 1)
                            } while (!trigger.dayOfWeek!!.contains(get(Calendar.DAY_OF_WEEK) - 1)) // daysOfWeek: 0 (Sun) - 6 (Sat), Calendar.DAY_OF_WEEK: 1 (Sun) - 7 (Sat)


                        }

                    }
                }
                "monthly" -> {
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.MONTH, 1)
                    }
                }
            }

            when (frequency) {
                "daily" -> {
                    // No additional adjustments needed
                }
                "weekly" -> {
                    if(trigger.dayOfWeek != null && trigger.dayOfWeek!!.isNotEmpty()) {
                        val dayOfWeek = trigger.dayOfWeek!!.minOrNull() ?: return // daysOfWeek: 0 (Sun) - 6 (Sat)
                        val targetDayOfWeek = (dayOfWeek + 1) // Calendar.DAY_OF_WEEK: 1 (Sun) - 7 (Sat)
                        while (get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
                            add(Calendar.DAY_OF_WEEK, 1)
                        }
                    }
                }
                "monthly" -> {
                    val dayOfMonth = (trigger.data["dayOfMonth"] as? Number)?.toInt() ?: 1
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    if (get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                }
            }
        }.timeInMillis

        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)

        Timber.d("Programando alarma ${trigger.frequency} ${trigger.type} para ${trigger.hour}:${trigger.minute} (${routine.name}), id: $alarmId, time: ${calendar.time}")

        if (canScheduleExactAlarms()) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                300_000, // 5 minutos de ventana
                pendingIntent
            )
            Timber.w("No se tienen permisos para alarmas exactas. Se usará una ventana de 5 minutos.")
        }
    }


    fun cancel(routine: Routine) {
        Timber.d("Cancelando alarmas para la rutina: ${routine.uuid}, ${routine.name}")
        routine.triggers.filter { it.type == Trigger.TriggerType.TIME.name }.forEach { trigger ->
            cancelTimeTrigger(routine, trigger)
        }
    }

    private fun cancelTimeTrigger(routine: Routine, trigger: Trigger) {
        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)
        alarmManager.cancel(pendingIntent)
        Timber.d("Alarma ${trigger.frequency} ${trigger.type} cancelada para ${routine.name}, id: $alarmId")
    }

    private fun generateAlarmId(routine: Routine, trigger: Trigger): Int {
        // Combine routine UUID and a trigger identifier to create a unique ID
        return "${routine.uuid}-${trigger.uuid}".hashCode()
    }

    // TODO: This method MUST be replaced with the actual implementation to retrieve a RoutineEntity
    // from your Room database based on the UUID.
    // You need to use your Room DAO (e.g., routineDao) to fetch the RoutineEntity.
    // Example: return routineDao.getRoutineByUuid(uuid)
    private fun getRoutineEntityByUuid(uuid: String): RoutineEntity {
        // Example (replace with your Room DAO): routineDao.getRoutineByUuid(uuid)
        throw NotImplementedError("getRoutineEntityByUuid(uuid) is not implemented yet.")
    }

    private fun createPendingIntent(routine: Routine, alarmId: Int): PendingIntent {
        //  Obtener la RoutineEntity a partir del UUID.  Adaptar esto según tu implementación.
        val routineEntity = getRoutineEntityByUuid(routine.uuid)  //  Asumiendo que este método existe
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId).putExtra(AlarmReceiver.EXTRA_ROUTINE, routineEntity)  //  Pasar la RoutineEntity
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun rescheduleAll() {
        //TODO: Need to implement this properly, get all active routines
        Timber.d("Reprogramando todas las alarmas...")
        // TODO: Obtener todas las rutinas activas del ViewModel o Repository
        //  y llamar a schedule(routine) para cada una
        //  Por ahora, un ejemplo con una rutina হার্ডcodeada
        //  Esto se DEBE cambiar
        /*val rutinaHardcodeada = Routine(
            uuid = "123e4567-e89b-12d3-a456-426614174000",
            name = "Rutina de prueba",
            triggers = listOf(
                Trigger(
                    type = Trigger.TriggerType.TIME.name,
                    hour = 8,
                    minute = 0,
                    frequency = "daily"
                )
            ),
            actions = emptyList(),
            isActive = true
        )
        schedule(rutinaHardcodeada)*/
        Timber.d("La reprogramación total de alarmas está implementada a medias. Ver TODO.")
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // En versiones anteriores a Android 12, no hay restricciones
        }
    }
}
