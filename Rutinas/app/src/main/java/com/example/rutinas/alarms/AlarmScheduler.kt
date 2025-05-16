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
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneId

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineRepository: RoutineRepository
) {

    internal val alarmManager: AlarmManager = // Changed to internal
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

        // Schedule TIME triggers
        routine.triggers.filter { it.triggerType == "TIME" }
            .forEach { trigger -> scheduleTimeTrigger(routine, trigger) }

        // Schedule CALENDAR triggers
        routine.triggers.filter { it.triggerType == "CALENDAR" }
            .forEach { trigger -> scheduleCalendarTrigger(routine, trigger) }
    }

    internal fun scheduleTimeTrigger(routine: Routine, trigger: Trigger) { // Changed to internal
        val dataWrapper = trigger.data ?: return
        val mapData = dataWrapper.data ?: return

        val hour = mapData["hour"] as? Int
        val minute = mapData["minute"] as? Int
        val frequency = mapData["frequency"] as? String ?: "daily"

        if (hour == null || minute == null) {
            Timber.e("Datos de hora o minuto faltantes para el trigger de tiempo: ${trigger.uuid}")
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            when (frequency) {
                "daily" -> if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
                "weekly" -> {
                    val daysOfWeek = mapData["daysOfWeek"] as? List<Int>
                    if (daysOfWeek.isNullOrEmpty()) {
                        Timber.e("Días de la semana faltantes para el trigger semanal: ${trigger.uuid}")
                        return@apply
                    }
                    // Find the next upcoming selected day of the week
                    while (timeInMillis <= System.currentTimeMillis() ||
                        !daysOfWeek.contains(get(Calendar.DAY_OF_WEEK) - 1)
                    ) {
                        add(Calendar.DAY_OF_YEAR, 1) // Advance by a day
                    }
                }
                "monthly" -> {
                    val dayOfMonth = (mapData["dayOfMonth"] as? Number)?.toInt() ?: 1
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    // If the set date is in the past, move to the next month
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.MONTH, 1)
                        // Ensure the day of month is valid for the next month
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        if (get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        }
                    }
                    // If the day of month is not valid for the current month, move to the next valid day
                    if (get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                }
            }
        }

        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)

        Timber.d("Programando alarma $frequency para $hour:$minute (${routine.name}), id: $alarmId, time: ${calendar.time}")

        if (canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 300_000, pendingIntent)
            Timber.w("Sin permisos para alarmas exactas; usando ventana de 5 minutos.")
        }
    }

    private fun scheduleCalendarTrigger(routine: Routine, trigger: Trigger) {
        val dataWrapper = trigger.data ?: return
        val mapData = dataWrapper.data ?: return

        val year = mapData["year"] as? Int
        val month = mapData["month"] as? Int // 0-indexed month
        val day = mapData["day"] as? Int

        if (year == null || month == null || day == null) {
            Timber.e("Datos de fecha faltantes para el trigger de calendario: ${trigger.uuid}")
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0) // Default to start of the day
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Only schedule if the date is in the future
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            Timber.d("Fecha del trigger de calendario (${trigger.uuid}) es en el pasado. No se programará.")
            return
        }

        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)

        Timber.d("Programando alarma de calendario para ${year}-${month + 1}-${day} (${routine.name}), id: $alarmId, time: ${calendar.time}")

        if (canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 300_000, pendingIntent)
            Timber.w("Sin permisos para alarmas exactas; usando ventana de 5 minutos.")
        }
    }

    suspend fun cancel(routine: Routine) {
        Timber.d("Cancelando alarmas para la rutina: ${routine.uuid}, ${routine.name}")
        routine.triggers.forEach { trigger ->
            val alarmId = generateAlarmId(routine, trigger)
            val pendingIntent = createPendingIntent(routine, alarmId)
            alarmManager.cancel(pendingIntent)
            Timber.d("Alarma cancelada para ${routine.name}, id: $alarmId, tipo: ${trigger.triggerType}")
        }
    }

    // This function only generates the alarm ID
    private fun generateAlarmId(routine: Routine, trigger: Trigger): Int {
        // Combine routine UUID and a trigger identifier to create a unique ID
        return "${routine.uuid}-${trigger.uuid}".hashCode()
    }

    internal fun createPendingIntent(routine: Routine, alarmId: Int): PendingIntent { // Changed to internal
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_UUID, routine.uuid) // Pasar UUID
        }
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
}
