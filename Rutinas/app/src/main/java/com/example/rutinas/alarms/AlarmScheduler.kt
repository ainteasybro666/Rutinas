package com.example.rutinas.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.domain.Routine
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.receivers.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import java.time.DayOfWeek
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineRepository: RoutineRepository
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun schedule(routine: Routine) {
        Timber.d("Programando alarmas para la rutina: ${routine.uuid}, ${routine.name}")

        // Cancelar alarmas existentes antes de programar nuevas
        cancel(routine)

        // Only schedule if the routine is enabled
        if (!routine.isEnabled) {
            Timber.d("Rutina ${routine.name} (${routine.uuid}) está deshabilitada. No se programarán alarmas.")
            return
        }

        routine.triggers.forEach { trigger ->
            when (trigger.triggerType) {
                "TIME" -> scheduleTimeTrigger(routine, trigger)
                "CALENDAR" -> scheduleCalendarTrigger(routine, trigger) // <<-- NUEVO: Manejar CALENDAR
                // Add other trigger types here in the future
                else -> Timber.w("Tipo de trigger desconocido: ${trigger.triggerType} para rutina ${routine.name}")
            }
        }
    }

    // <<-- NUEVO: Implementación para triggers de Calendario -->>
    private fun scheduleCalendarTrigger(routine: Routine, trigger: Trigger) {
        val dataWrapper = trigger.data ?: run {
            Timber.e("Calendar trigger data is null for routine ${routine.name}")
            return
        }
        val mapData = dataWrapper.data ?: run {
            Timber.e("Calendar trigger map data is null for routine ${routine.name}")
            return
        }

        // Assuming date and time are stored in a way that can be parsed into LocalDateTime
        // For example, as "year", "month", "day", "hour", "minute" integers
        val year = mapData["year"] as? Int ?: run {
            Timber.e("Calendar trigger missing year data for routine ${routine.name}")
            return
        }
        val month = mapData["month"] as? Int ?: run { // Month is 1-indexed
            Timber.e("Calendar trigger missing month data for routine ${routine.name}")
            return
        }
        val day = mapData["day"] as? Int ?: run {
            Timber.e("Calendar trigger missing day data for routine ${routine.name}")
            return
        }
        val hour = mapData["hour"] as? Int ?: run {
            Timber.e("Calendar trigger missing hour data for routine ${routine.name}")
            return
        }
        val minute = mapData["minute"] as? Int ?: run {
            Timber.e("Calendar trigger missing minute data for routine ${routine.name}")
            return
        }

        try {
            val triggerDateTime = LocalDateTime.of(year, month, day, hour, minute)
            val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Only schedule if the trigger time is in the future
            if (triggerMillis > System.currentTimeMillis()) {
                val alarmId = generateAlarmId(routine, trigger)
                val pendingIntent = createPendingIntent(routine, alarmId)

                Timber.d("Programando alarma de CALENDARIO para ${routine.name}, fecha: $triggerDateTime, id: $alarmId")

                // Calendar triggers are usually one-time events, so setExact is appropriate
                if (canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    // Fallback for devices without exact alarm permission
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerMillis, 60000, pendingIntent) // Use a small window
                    Timber.w("Sin permisos para alarmas exactas; usando ventana pequeña para trigger de calendario.")
                }
            } else {
                Timber.d("Calendar trigger time for routine ${routine.name} is in the past. Skipping scheduling.")
            }

        } catch (e: Exception) {
            Timber.e(e, "Error scheduling calendar trigger for routine ${routine.name}")
        }
    }
    // <<-- FIN NUEVO -->>


    private fun scheduleTimeTrigger(routine: Routine, trigger: Trigger) {
        val dataWrapper = trigger.data ?: run {
            Timber.e("Time trigger data is null for routine ${routine.name}")
            return
        }
        val mapData = dataWrapper.data ?: run {
            Timber.e("Time trigger map data is null for routine ${routine.name}")
            return
        }

        val hour = mapData["hour"] as? Int ?: run {
            Timber.e("Time trigger missing hour data for routine ${routine.name}")
            return
        }
        val minute = mapData["minute"] as? Int ?: run {
            Timber.e("Time trigger missing minute data for routine ${routine.name}")
            return
        }
        val frequency = mapData["frequency"] as? String ?: "daily"

        // <<-- NUEVO: Usar LocalDateTime para calcular la próxima ocurrencia -->>
        val now = LocalDateTime.now()
        var nextTriggerTime = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        when (frequency) {
            "daily" -> {
                if (nextTriggerTime.isBefore(now) || nextTriggerTime.isEqual(now)) {
                    nextTriggerTime = nextTriggerTime.plusDays(1)
                }
            }
            "weekly" -> {
                val daysOfWeek = mapData["daysOfWeek"] as? List<Int> ?: run {
                    Timber.e("Time trigger (weekly) missing daysOfWeek data for routine ${routine.name}")
                    return
                }
                // Convert Room's 0-6 (Sunday-Saturday) to java.time.DayOfWeek (Monday-Sunday)
                val javaTimeDaysOfWeek = daysOfWeek.map { (it % 7) + 1 }.map { DayOfWeek.of(it) }

                // Find the next target day of the week ON or AFTER the current day/time
                var foundNext = false
                // Start checking from today
                var checkDate = nextTriggerTime
                if (checkDate.isBefore(now) || checkDate.isEqual(now)) {
                    checkDate = checkDate.plusDays(1) // If time today is in the past, start checking from tomorrow
                }

                for (i in 0..7) { // Check up to a week ahead
                    if (javaTimeDaysOfWeek.contains(checkDate.dayOfWeek)) {
                        nextTriggerTime = checkDate
                        foundNext = true
                        break
                    }
                    checkDate = checkDate.plusDays(1)
                }

                if (!foundNext) {
                    Timber.e("Could not find a next weekly trigger day for routine ${routine.name}")
                    return // Should not happen if daysOfWeek is not empty
                }
            }
            "monthly" -> {
                val dayOfMonth = (mapData["dayOfMonth"] as? Number)?.toInt() ?: run {
                    Timber.e("Time trigger (monthly) missing dayOfMonth data for routine ${routine.name}")
                    return
                }

                try {
                    nextTriggerTime = nextTriggerTime.withDayOfMonth(dayOfMonth)
                } catch (e: Exception) {
                    // Handle cases where the dayOfMonth is invalid for the current month (e.g., 31 in February)
                    Timber.w(e, "Day $dayOfMonth is invalid for the current month. Adjusting.")
                    nextTriggerTime = nextTriggerTime.with(TemporalAdjusters.lastDayOfMonth())
                }


                if (nextTriggerTime.isBefore(now) || nextTriggerTime.isEqual(now)) {
                    nextTriggerTime = nextTriggerTime.plusMonths(1)
                    // Re-adjust day of month if it was the last day of the previous month and
                    // the next month has fewer days.
                    try {
                        nextTriggerTime = nextTriggerTime.withDayOfMonth(dayOfMonth)
                    } catch (e: Exception) {
                        Timber.w(e, "Day $dayOfMonth is invalid for the next month. Adjusting to last day of next month.")
                        nextTriggerTime = nextTriggerTime.with(TemporalAdjusters.lastDayOfMonth())
                    }
                }
            }
            else -> {
                Timber.w("Frecuencia de trigger de tiempo desconocida: $frequency para rutina ${routine.name}")
                return
            }
        }

        val nextTriggerMillis = nextTriggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)

        Timber.d("Programando alarma de TIEMPO $frequency para ${routine.name} (${hour}:${minute}), próximo disparo: $nextTriggerTime, id: $alarmId")


        // Use setExact or setWindow
        if (canScheduleExactAlarms()) {
            // Use setExactAndAllowWhileIdle for potentially more reliable alarms in Doze mode
            // Be mindful of battery implications. If not critical to be exact, use setExact.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent)
            }
        } else {
            // Fallback for devices without exact alarm permission
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, nextTriggerMillis, 300_000, pendingIntent) // 5-minute window
            Timber.w("Sin permisos para alarmas exactas; usando ventana de 5 minutos para trigger de tiempo.")
        }
        // <<-- FIN NUEVO -->>
    }

    suspend fun cancel(routine: Routine) {
        Timber.d("Cancelando alarmas para la rutina: ${routine.uuid}, ${routine.name}")
        routine.triggers.forEach { trigger -> // <<-- Modificado para iterar sobre todos los triggers -->>
            cancelTrigger(routine, trigger) // <<-- Llamar a una función genérica de cancelación -->>
        }
    }

    // <<-- NUEVO: Función genérica para cancelar cualquier tipo de trigger -->>
    private fun cancelTrigger(routine: Routine, trigger: Trigger) {
        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)
        alarmManager.cancel(pendingIntent)
        Timber.d("Alarma cancelada para ${routine.name}, trigger id: $alarmId")
    }
    // <<-- FIN NUEVO -->>

    // CORRECTED: This function only generates the alarm ID
    private fun generateAlarmId(routine: Routine, trigger: Trigger): Int {
        // Combine routine UUID and a trigger identifier to create a unique ID
        // Using trigger.uuid is better than a simple hash of the map data
        // Consider using routine.id and trigger.id once they are available from the DB
        return "${routine.uuid}-${trigger.uuid}".hashCode()
    }

    // CORRECTED: This function creates the PendingIntent
    private fun createPendingIntent(routine: Routine, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_UUID, routine.uuid) // Pasar UUID
            // Potentially add trigger UUID as well if needed in the receiver
            //putExtra("TRIGGER_UUID", trigger.uuid) // <<-- Opcional: pasar UUID del trigger
        }
        // Use FLAG_UPDATE_CURRENT to update the extra data if the same alarmId is used
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    suspend fun rescheduleAll() {
        Timber.d("Reprogramando todas las alarmas...")
        routineRepository.getAllRoutines().collect{ routines ->
            routines.filter { it.isEnabled }.forEach{ routine ->
                schedule(routine)
            }
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    // <<-- NUEVO: Función para solicitar permiso de alarmas exactas -->>
    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Timber.d("Solicitando permiso para alarmas exactas...")
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Necesario si se llama desde un contexto que no es Activity
                context.startActivity(intent)
            }
        }
    }
    // <<-- FIN NUEVO -->>
}