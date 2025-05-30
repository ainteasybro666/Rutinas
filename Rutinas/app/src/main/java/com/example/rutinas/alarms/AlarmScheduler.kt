package com.example.rutinas.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.rutinas.data.model.FrequencyType
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.domain.Routine
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.receivers.AlarmReceiver
import com.example.rutinas.ui.edit.dialogs.TriggerTypeDialog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import java.lang.System.currentTimeMillis
import java.time.DayOfWeek
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Date
import kotlin.math.absoluteValue

@Singleton
class AlarmScheduler @Inject constructor(
        @ApplicationContext private val context: Context,
    private val routineRepository: RoutineRepository
) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    // Esta función programa todas las alarmas para una rutina dada
    fun schedule(routine: Routine) {
        Timber.d("Scheduling alarms for routine: ${routine.name} (${routine.uuid})")

        // Iterar sobre cada trigger asociado a la rutina
        for (trigger in routine.triggers) {
            Timber.d("Processing trigger: ${trigger.uuid} for routine: ${routine.name}")

            // <<-- PASO CLAVE: Calcular el tiempo de la próxima alarma -->>
            val nextAlarmTimeMillis = calculateNextAlarmTimeMillis(trigger, routine) // Pasa la rutina

            // <<-- CORRECCIÓN: Declara y asigna el valor a currentTimeMillis -->>
            val currentTimeMillis = System.currentTimeMillis() // ¡Aquí se define!

            // <<-- CORRECCIÓN: Ahora currentTimeMillis está definido y puede ser usado en el log -->>
            Timber.d("Schedule check: Trigger UUID: ${trigger.uuid}, Calculated nextAlarmTimeMillis: $nextAlarmTimeMillis, Current time millis: $currentTimeMillis")


            if (nextAlarmTimeMillis > System.currentTimeMillis()) {
                // Solo programar si el próximo tiempo está en el futuro
                val alarmId = generateAlarmId(routine.uuid, trigger.uuid) // Usar la sobrecarga con UUIDs
                val pendingIntent = createPendingIntent(alarmId, routine.uuid, trigger.uuid) // Crear un PendingIntent único para este trigger

                Timber.d("Scheduling alarm for routine: ${routine.name}, trigger: ${trigger.uuid} at: ${LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextAlarmTimeMillis), ZoneId.systemDefault())} with alarmId: $alarmId")

                // <<-- Usar AlarmManager para programar la alarma -->>
                // Usa RTC_WAKEUP para despertar el dispositivo si está en modo Doze
                alarmManager.setExactAndAllowWhileIdle( // setExactAndAllowWhileIdle es bueno para la precisión y Doze
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTimeMillis,
                    pendingIntent
                )
            } else {
                Timber.w("Skipping scheduling for trigger ${trigger.uuid}: next alarm time ($nextAlarmTimeMillis) is in the past or now compared to current time ($currentTimeMillis). Consider recalculating and scheduling the very next one if logic requires.")
                // TODO: Podrías necesitar lógica aquí para recalcular la próxima alarma si la calculada
                //  inicialmente está en el pasado. Esto depende de tu implementación de calculateNextAlarmTimeMillis.
            }
        }
        Timber.d("Finished scheduling for routine: ${routine.name}")
    }


    // Helper para calcular el tiempo de la próxima alarma en milisegundos Epoch
    private fun calculateNextAlarmTimeMillis(trigger: Trigger, routine: Routine): Long {
        Timber.d("Calculating next alarm time for trigger: ${trigger.uuid}, type: ${trigger.triggerType}")

        // Obtener la hora y minuto comunes para triggers de tiempo y calendario (si aplica)
        // Usa los métodos getter de DataWrapper que ya manejan Double a Int
        val hour = trigger.data.getInt("hour")
        val minute = trigger.data.getInt("minute")

        Timber.d("calculateNextAlarmTimeMillis: Retrieved hour: $hour (from DataWrapper), minute: $minute (from DataWrapper) for trigger ${trigger.uuid}")

        // Obtener la fecha de inicio y fin del trigger (si aplica)
        val startDate = trigger.startDate
        val endDate = trigger.endDate

        // Obtener la zona horaria del sistema
        val zoneId = ZoneId.systemDefault()

        // Obtener la fecha y hora actual en la zona horaria del sistema
        val now = ZonedDateTime.now(zoneId)

        // Variable para almacenar el tiempo calculado de la próxima alarma
        var nextAlarmTime: ZonedDateTime? = null

        // Verificar si la alarma está dentro del rango de fechas si startDate o endDate existen
        // <<-- CORRECCIÓN: Mover logging a este punto -->>
        if (startDate != null && now.toLocalDateTime().isBefore(startDate)) {
            Timber.d("calculateNextAlarmTimeMillis: Trigger start date is in the future ($startDate). Not scheduling yet for trigger UUID: ${trigger.uuid}.")
            return 0L // Start date is in the future, don't schedule yet
        }
        // <<-- CORRECCIÓN: Mover logging a este punto -->>
        if (endDate != null && now.toLocalDateTime().isAfter(endDate)) {
            Timber.d("calculateNextAlarmTimeMillis: Trigger end date is in the past ($endDate). Not scheduling for trigger UUID: ${trigger.uuid}.")
            return 0L // End date is in the past, don't schedule
        }


        when (trigger.triggerType) {
            TriggerTypeDialog.TriggerType.TIME -> {
                Timber.d("calculateNextAlarmTimeMillis: Handling TIME trigger")
                // Lógica para triggers de tipo TIME (diario, semanal, mensual)

                // Asegurarse de que hour y minute no sean nulos para triggers TIME
                if (hour == null || minute == null) {
                    Timber.e("calculateNextAlarmTimeMillis: TIME trigger is missing hour or minute data (getInt returned null) for trigger UUID: ${trigger.uuid}") // Ajusta el mensaje si quieres
                    return 0L
                }

                // <<-- CORRECCIÓN: Usar el getter robusto para frequency -->>
                val frequency = trigger.data.getFrequencyType("frequency")

                when (frequency) {
                    FrequencyType.ONCE -> {
                        Timber.d("calculateNextAlarmTimeMillis: Calculating for TIME trigger - Once frequency")
                        var nextAlarmCandidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

                        // Si la hora ya pasó hoy, programar para mañana
                        if (nextAlarmCandidate.isBefore(now)) {
                            nextAlarmCandidate = nextAlarmCandidate.plusDays(1)
                            Timber.d("calculateNextAlarmTimeMillis: TIME Once trigger time already passed today, scheduling for tomorrow.")
                        } else {
                            Timber.d("calculateNextAlarmTimeMillis: TIME Once trigger time is in the future today.")
                        }
                        nextAlarmTime = nextAlarmCandidate // Asignar a la variable unificada
                    }
                    FrequencyType.WEEKLY -> {
                        Timber.d("calculateNextAlarmTimeMillis: Calculating for TIME trigger - Weekly frequency")
                        // Usar el getter robusto para daysOfWeek
                        val daysOfWeek = trigger.data.getListOfInt("daysOfWeek")

                        if (daysOfWeek.isNullOrEmpty()) {
                            Timber.e("calculateNextAlarmTimeMillis: Weekly TIME trigger is missing daysOfWeek data or it's empty for trigger UUID: ${trigger.uuid}")
                            // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                            return 0L // Datos de días de la semana faltantes
                        }

                        // Convertir los enteros de días de la semana a DayOfWeek (Java time)
                        val javaDaysOfWeek = daysOfWeek.mapNotNull { dayInt ->
                            try {
                                when(dayInt) {
                                    0 -> DayOfWeek.SUNDAY
                                    1 -> DayOfWeek.MONDAY
                                    2 -> DayOfWeek.TUESDAY
                                    3 -> DayOfWeek.WEDNESDAY
                                    4 -> DayOfWeek.THURSDAY
                                    5 -> DayOfWeek.FRIDAY
                                    6 -> DayOfWeek.SATURDAY
                                    else -> {
                                        Timber.w("calculateNextAlarmTimeMillis: Invalid day of week integer found: $dayInt for trigger UUID: ${trigger.uuid}. Skipping.")
                                        null // Ignorar enteros inválidos
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "calculateNextAlarmTimeMillis: Error mapping day of week integer: $dayInt for trigger UUID: ${trigger.uuid}")
                                null
                            }
                        }.toSet() // Usar Set para búsquedas eficientes

                        if (javaDaysOfWeek.isEmpty()) {
                            Timber.e("calculateNextAlarmTimeMillis: No valid days of week found after mapping for weekly TIME trigger UUID: ${trigger.uuid}")
                            // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                            return 0L // No hay días válidos para programar después del mapeo
                        }

                        // Encontrar la próxima ocurrencia en los días de la semana especificados
                        var nextAlarmCandidate: ZonedDateTime? = null
                        var currentCheckDay = now.toLocalDate() // Empezamos buscando desde hoy

                        // Buscar en los próximos 7 días para encontrar la próxima ocurrencia
                        for (i in 0..7) {
                            val potentialDate = currentCheckDay.plusDays(i.toLong())
                            val potentialDayOfWeek = potentialDate.dayOfWeek

                            if (javaDaysOfWeek.contains(potentialDayOfWeek)) {
                                // Este día de la semana es uno de los días seleccionados
                                val timeAtPotentialDate = potentialDate.atTime(hour, minute).atZone(zoneId)

                                // Si es hoy y la hora ya pasó, buscar el siguiente día de la semana válido
                                // Si es un día futuro, la hora siempre es "válida" en el futuro.
                                if (timeAtPotentialDate.isAfter(now)) {
                                    nextAlarmCandidate = timeAtPotentialDate
                                    break // Encontramos la próxima hora válida
                                }
                                // Si es hoy y la hora ya pasó, el bucle continuará buscando el siguiente día válido
                            }
                            // Si el día actual no es un día seleccionado, el bucle pasará al siguiente día
                        }
                        nextAlarmTime = nextAlarmCandidate // Asignar a la variable unificada
                    }
                    FrequencyType.MONTHLY -> {
                        Timber.d("calculateNextAlarmTimeMillis: Calculating for TIME trigger - Monthly frequency")
                        // Usar el getter robusto para dayOfMonth
                        val dayOfMonth = trigger.data.getInt("dayOfMonth")

                        if (dayOfMonth == null || dayOfMonth !in 1..31) {
                            Timber.e("calculateNextAlarmTimeMillis: Monthly TIME trigger has invalid or missing dayOfMonth data ($dayOfMonth) for trigger UUID: ${trigger.uuid}")
                            // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                            return 0L // Datos de día del mes faltantes o inválidos
                        }

                        // Obtener la fecha actual y el mes actual
                        val currentYear = now.year
                        val currentMonth = now.monthValue

                        // Intentar crear una fecha para el día y hora especificados en el mes actual
                        var nextAlarmCandidate: ZonedDateTime? = try {
                            LocalDateTime.of(currentYear, currentMonth, dayOfMonth, hour, minute)
                                .atZone(zoneId)
                        } catch (e: Exception) {
                            // Manejar casos donde el día del mes no existe en el mes actual (ej: 31 de Febrero)
                            Timber.w(e, "calculateNextAlarmTimeMillis: Could not create LocalDateTime for monthly trigger date in current month: $currentYear-$currentMonth-$dayOfMonth at $hour:$minute for trigger UUID: ${trigger.uuid}. Checking next month.")
                            null // No se pudo crear la fecha en el mes actual
                        }

                        // Si la fecha candidata es nula (día inválido para el mes) o ya pasó, buscar en el próximo mes
                        if (nextAlarmCandidate == null || nextAlarmCandidate.isBefore(now)) {
                            Timber.d("calculateNextAlarmTimeMillis: Monthly trigger date in current month already passed or is invalid. Checking next month.")
                            // Mover al próximo mes
                            val nextMonth = now.toLocalDate().plusMonths(1)
                            val nextYear = nextMonth.year
                            val nextMonthValue = nextMonth.monthValue

                            // Intentar crear una fecha para el día y hora especificados en el próximo mes
                            nextAlarmCandidate = try {
                                LocalDateTime.of(nextYear, nextMonthValue, dayOfMonth, hour, minute)
                                    .atZone(zoneId)
                            } catch (e: Exception) {
                                // Manejar casos donde el día del mes no existe en el próximo mes
                                Timber.e(e, "calculateNextAlarmTimeMillis: Could not create LocalDateTime for monthly trigger date in next month: $nextYear-$nextMonthValue-$dayOfMonth at $hour:$minute for trigger UUID: ${trigger.uuid}. This trigger might not be schedulable.")
                                null // Fallback si el día es inválido incluso en el próximo mes (ej: 31 de un mes de 30 días)
                            }
                            Timber.d("calculateNextAlarmTimeMillis: Retrieved hour: $hour, minute: $minute, dayOfMonth: $dayOfMonth for trigger ${trigger.uuid}")
                        }
                        nextAlarmTime = nextAlarmCandidate // Asignar a la variable unificada
                    }
                    else -> {
                        Timber.e("calculateNextAlarmTimeMillis: Unknown frequency type for TIME trigger: $frequency for trigger UUID: ${trigger.uuid}")
                        // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                        return 0L // Frecuencia desconocida
                    }
                }
            }
            TriggerTypeDialog.TriggerType.CALENDAR -> {
                Timber.d("calculateNextAlarmTimeMillis: Handling CALENDAR trigger")
                // Lógica para triggers de tipo CALENDAR (una fecha y hora específica)

                // <<-- CORRECCIÓN: Usar los métodos getter robustos -->>
                val year = trigger.data.getInt("year")
                val month = trigger.data.getInt("month") // 1-12
                val day = trigger.data.getInt("day")

                // Asegurarse de que todos los datos de fecha/hora estén presentes
                if (year == null || month == null || day == null || hour == null || minute == null) {
                    Timber.e("calculateNextAlarmTimeMillis: CALENDAR trigger is missing date or time data for trigger UUID: ${trigger.uuid}")
                    // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                    return 0L // Datos faltantes
                }

                // Intentar crear la fecha y hora del trigger
                val triggerDateTime = try {
                    LocalDateTime.of(year, month, day, hour, minute)
                } catch (e: Exception) {
                    Timber.e(e, "calculateNextAlarmTimeMillis: Invalid date/time data for CALENDAR trigger: $year-$month-$day at $hour:$minute for trigger UUID: ${trigger.uuid}")
                    // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                    return 0L // Datos de fecha/hora inválidos
                }

                // Convertir a ZonedDateTime para comparación y obtener milisegundos
                val triggerZonedDateTime = triggerDateTime.atZone(zoneId)

                // Si la fecha y hora del trigger ya pasaron, no programar
                if (triggerZonedDateTime.isBefore(now)) {
                    Timber.d("calculateNextAlarmTimeMillis: CALENDAR trigger date and time have already passed for trigger UUID: ${trigger.uuid}")
                    // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                    return 0L // Ya pasó
                }

                // Si la fecha y hora del trigger están en el futuro, asignar a la variable unificada
                nextAlarmTime = triggerZonedDateTime
            }
            TriggerTypeDialog.TriggerType.LOCATION -> {
                Timber.d("calculateNextAlarmTimeMillis: Handling LOCATION trigger - Not programmable with AlarmManager")
                // Los triggers de tipo LOCATION no se programan con AlarmManager.
                // Su lógica de activación se maneja por separado (BroadcastReceiver, Service, etc.).
                // <<-- CORRECCIÓN: Retorna 0L aquí -->>
                return 0L // Devuelve 0L para indicar que no hay una hora fija para programar
            }
            // No necesitamos un 'else' si hemos manejado todos los casos de la enumeración
        }

        // <<-- CORRECCIÓN: Logging del valor final de retorno -->>
        val finalNextAlarmTimeMillis = nextAlarmTime?.toInstant()?.toEpochMilli() ?: 0L
        if (finalNextAlarmTimeMillis > 0L) {
            Timber.d("calculateNextAlarmTimeMillis: Successfully calculated next alarm time for trigger ${trigger.uuid}: ${Date(finalNextAlarmTimeMillis)} (Millis: $finalNextAlarmTimeMillis)")
        } else {
            Timber.w("calculateNextAlarmTimeMillis: Could not calculate a valid future alarm time for trigger ${trigger.uuid}. Returning 0L.")
        }

        return finalNextAlarmTimeMillis // Retornar el valor unificado (o 0L si fue null)
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
                val alarmId = generateAlarmId(routine.uuid, trigger.uuid)
                val pendingIntent = createPendingIntent(alarmId, routine.uuid, trigger.uuid)

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


    fun scheduleTimeTrigger(routine: Routine, trigger: Trigger) {
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

        val alarmId = generateAlarmId(routine.uuid, trigger.uuid)
        val pendingIntent = createPendingIntent(alarmId, routine.uuid, trigger.uuid)

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

    // Function that was likely causing the original suspend error - check its implementation
    // Assuming this function exists and is called by scheduleRoutineAlarms
    // If it only contains non-blocking AlarmManager calls, it doesn't need to be suspend.
    // If it reads from the database, it does.
    // Let's assume it *might* need to be suspend for now if it interacts with the repository
    suspend fun cancel(routine: Routine) {
        Timber.d("AlarmScheduler: Cancelling all alarms for routine: ${routine.uuid}")
        // TODO: Implement cancellation logic here if this function is intended to cancel *all* alarms
        // This could potentially just call cancelRoutineAlarms(routine)

        // Example: If this function was originally intended to read all triggers and cancel them:
        val triggers = routineRepository.getTriggersForRoutine(routine.id) // This is likely a suspend call

        triggers.forEach { trigger: Trigger -> 
            cancelSingleAlarm(routine.uuid, trigger.uuid.toString())
        }
        Timber.d("AlarmScheduler: Finished attempting to cancel all alarms for routine: ${routine.uuid}")

    }

    // Función para cancelar una alarma específica por UUID de rutina y UUID de trigger
    // Signature based on the service calling cancelSingleAlarm(routine.uuid, trigger.uuid)
    fun cancelSingleAlarm(routineUuid: String, triggerUuid: String) {
        Timber.d("AlarmScheduler: Attempting to cancel single alarm for routine UUID: $routineUuid, trigger UUID: $triggerUuid")

        // Calcular el alarmId de la misma manera que se hizo al programar
        val alarmId = generateAlarmId(routineUuid, triggerUuid)
        Timber.d("AlarmScheduler: Calculated alarm ID for cancellation: $alarmId")

        // Crear el PendingIntent idéntico al que se usó para programar
        // Asegúrate de que createPendingIntentForCancellation use los mismos extras que createPendingIntent
        val pendingIntent = createPendingIntentForCancellation(routineUuid, triggerUuid, alarmId)

        // Cancelar la alarma
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.cancel(pendingIntent)
            Timber.i("AlarmScheduler: Successfully requested cancellation for alarm ID: $alarmId")
        } catch (e: SecurityException) {
            // Esto puede ocurrir si la app perdió los permisos o si hay algún problema con el PendingIntent
            Timber.e(e, "AlarmScheduler: SecurityException while attempting to cancel alarm ID: $alarmId. Check permissions.")
        } catch (e: Exception) {
            Timber.e(e, "AlarmScheduler: Error canceling alarm ID: $alarmId")
        }

        // Aunque AlarmManager.cancel() marca el PendingIntent para cancelación,
        // a veces puede ser útil llamar a pendingIntent.cancel() también,
        // especialmente si quieres liberar recursos asociados a ese PendingIntent.
        // pendingIntent?.cancel() // Esto es opcional y depende de si necesitas invalidar el PendingIntent de inmediato.
    }

    // Esta función cancela todas las alarmas asociadas a una rutina específica.
// Es útil cuando deshabilitas o eliminas una rutina.
    fun cancelRoutineAlarms(routine: Routine) {
        Timber.d("AlarmScheduler: Attempting to cancel all alarms for routine: ${routine.name} (${routine.uuid})")
        if (routine.triggers.isEmpty()) {
            Timber.d("AlarmScheduler: Routine has no triggers, nothing to cancel.")
            return
        }

        routine.triggers.forEach { trigger ->
            // Para cada trigger de la rutina, llamamos a cancelSingleAlarm
            // Esto cancelará la alarma específica asociada a este trigger.
            // Los triggers de tipo LOCATION (si no se programan con AlarmManager)
            // no tendrán una PendingIntent programada con AlarmManager,
            // por lo que calling cancelSingleAlarm para ellos no tendrá efecto en AlarmManager,
            // lo cual es el comportamiento deseado.
            cancelSingleAlarm(routine.uuid, trigger.uuid)
        }
        Timber.i("AlarmScheduler: Completed attempts to cancel alarms for routine: ${routine.name} (${routine.uuid})")
    }

    // Función para cancelar una alarma específica por UUID de rutina y Alarm ID
    // Signature based on the service calling cancelSingleAlarmByInfo(routineUuid, alarmId)
    fun cancelSingleAlarmByInfo(routineUuid: String, triggerUuid: String, alarmId: Int) {
        Timber.d("AlarmScheduler: Cancelling single alarm by info for routine UUID: $routineUuid, alarm ID: $alarmId")
        // TODO: Implement actual cancellation logic.
        //  This is similar to cancelSingleAlarm, but you already have the alarmId.
        val pendingIntent = createPendingIntentForCancellation(routineUuid, triggerUuid, alarmId) // Ahora triggerUuid existe

        pendingIntent?.cancel()
        Timber.d("AlarmScheduler: Attempted to cancel alarm with ID: $alarmId")
    }
    // <<-- FIN NUEVO -->>

    // Versión que acepta objetos Routine y Trigger (usada al programar)
    private fun generateAlarmId(routine: Routine, trigger: Trigger): Int {
        // Lógica para generar el ID a partir de los objetos
        // Asegúrate de que la lógica aquí y en la otra versión sea CONSISTENTE
        return "${routine.uuid}-${trigger.uuid}".hashCode().absoluteValue // O lógica similar
    }

    // <<-- AÑADE ESTA VERSIÓN (SOBRECARGA) -->>
// Versión que acepta UUIDs como String (usada al cancelar)
    private fun generateAlarmId(routineUuid: String, triggerUuid: String): Int {
        // ¡La lógica DEBE ser exactamente la misma que la versión que acepta objetos!
        // Solo trabaja con los Strings directamente.
        return "${routineUuid}-${triggerUuid}".hashCode().absoluteValue // MISMA LÓGICA
    }

    // CORRECTED: This function creates the PendingIntent
    private fun createPendingIntent(alarmId: Int, routineUuid: String, triggerUuid: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_UUID, routineUuid) // Usa el parámetro routineUuid
            putExtra(AlarmReceiver.EXTRA_TRIGGER_UUID, triggerUuid) // Usa el parámetro triggerUuid (asumiendo que quieres pasarlo)
        }
        // Use FLAG_UPDATE_CURRENT to update the extra data if the same alarmId is used
        // FLAG_IMMUTABLE is required in recent Android versions
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPendingIntentForCancellation(routineUuid: String, triggerUuid: String, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_UUID, routineUuid)
            putExtra(AlarmReceiver.EXTRA_TRIGGER_UUID, triggerUuid)
        }

        // <<<--- Cambia los flags aquí para que coincidan con createPendingIntent --->>>
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // El request code DEBE ser el mismo que el alarmId
        val requestCode = alarmId // Esto ya está correcto

        return PendingIntent.getBroadcast(
            context,
            requestCode, // Usar alarmId como request code
            intent,
            flags // <<<--- Usar los mismos flags --->>>
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
            // Si ya tenemos el permiso, no necesitamos hacer nada.
            if (alarmManager.canScheduleExactAlarms()) {
                Timber.d("AlarmScheduler: Exact alarm permission already granted.")
                return
            }

            Timber.d("AlarmScheduler: Requesting exact alarm permission.")
            // Crea un Intent para llevar al usuario a la pantalla de configuración
            // donde puede conceder el permiso.
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Opcional: Iniciar en una nueva tarea si se llama desde fuera de una actividad
            }
            context.startActivity(intent)
        } else {
            Timber.d("AlarmScheduler: Exact alarm permission not required on this Android version.")
        }
    }

    /**
     * Schedules alarms for the given routine based on its Time and Calendar triggers.
     * @param routine The Routine object for which to schedule alarms.
     */
    suspend fun scheduleRoutineAlarms(routine: Routine) { // Added 'suspend'
        Timber.d("AlarmScheduler: Scheduling alarms for routine: ${routine.name} (UUID: ${routine.uuid})")

        // 1. Cancelar alarmas existentes para esta rutina
        cancel(routine) // Now this call is valid

        // 2. Iterar sobre los triggers
        routine.triggers.forEach { trigger ->
            when (trigger.triggerType) {
                TriggerTypeDialog.TriggerType.TIME -> {
                    // 3, 4, 5: Programar trigger de tiempo
                    // Llama a tu función existente o crea una nueva si es necesario
                    // scheduleTimeTrigger(routine, trigger)
                    // Tu AlarmReceiver sugiere que ya tienes una función scheduleTimeTrigger
                    Timber.d("AlarmScheduler: Scheduling Time trigger: ${trigger.uuid}")
                    scheduleTimeTrigger(routine, trigger) // Llama a tu método existente
                }
                TriggerTypeDialog.TriggerType.CALENDAR -> {
                    // TODO: Implementar lógica para programar triggers de calendario
                    Timber.d("AlarmScheduler: Scheduling Calendar trigger: ${trigger.uuid} (TODO)")
                    // scheduleCalendarTrigger(routine, trigger) // Necesitarás crear esta función
                }
                // Añadir otros tipos de triggers que necesiten programación de alarma
                else -> {
                    Timber.d("AlarmScheduler: Trigger type ${trigger.triggerType} does not require scheduling.")
                }
            }
        }
        Timber.d("AlarmScheduler: Finished scheduling alarms for routine: ${routine.name}")
    }

    /**
     * Schedules a single alarm for a TIME trigger.
     * @param routineId The ID of the routine.
     * @param trigger The Time trigger.
     */
    private fun scheduleTimeTriggerAlarm(routineId: Long, trigger: Trigger) {
        Timber.d("AlarmScheduler: Scheduling Time trigger alarm for routine $routineId, trigger ${trigger.uuid}")
        // Aquí necesitas parsear los datos del trigger para obtener la hora, minutos, etc.
        // Y crear un PendingIntent para el BroadcastReceiver que manejará la alarma.
        // Usa alarmManager.setExactAndAllowWhileIdle() o alarmManager.setExact()
        // dependiendo de si necesitas que se dispare incluso en modo Doze.

        // Ejemplo básico (necesitarás adaptar esto a la estructura de tus datos de trigger):
        val triggerData = trigger.data.data // Asumiendo que los datos están en un mapa dentro de DataWrapper
        val hour = triggerData["hour"] as? Int ?: return // Obtén la hora de los datos
        val minute = triggerData["minute"] as? Int ?: return // Obtén los minutos de los datos
        // ... obtén otros datos necesarios como días de la semana si aplica ...

        // Calcula el tiempo en milisegundos para la próxima alarma
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, programa para mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            // TODO: Manejar días de la semana si el trigger los tiene configurados
        }

        val alarmTime = calendar.timeInMillis

        // Crea un Intent que será enviado cuando la alarma se dispare
        // Este Intent debe ser manejado por un BroadcastReceiver.
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // Pasa información necesaria al BroadcastReceiver, como el ID de la rutina
            putExtra("ROUTINE_ID", routineId)
            putExtra("TRIGGER_UUID", trigger.uuid.toString())
            // Añade otros datos del trigger o rutina que el receiver necesite
        }

        // Crea un PendingIntent que envuelve el Intent.
        // Usa un request code único para cada alarma, quizás basado en el ID de la rutina y el UUID del trigger.
        val requestCode = (routineId.toInt() * 1000 + trigger.uuid.hashCode()).absoluteValue // Ejemplo simple de request code
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Programa la alarma exacta
        if (canScheduleExactAlarms()) { // Verifica el permiso antes de programar
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, // Tipo de alarma que activa el dispositivo
                alarmTime,
                pendingIntent
            )
            Timber.d("AlarmScheduler: Exact alarm scheduled for Time trigger ${trigger.uuid} at ${Date(alarmTime)}")
        } else {
            Timber.e("AlarmScheduler: Cannot schedule exact alarm for Time trigger ${trigger.uuid}: Permission missing.")
            // Considerar notificar al usuario que la alarma no se programó correctamente.
        }
    }

    /**
     * Schedules a single alarm for a CALENDAR trigger.
     * @param routineId The ID of the routine.
     * @param trigger The Calendar trigger.
     */
    private fun scheduleCalendarTriggerAlarm(routineId: Long, trigger: Trigger) {
        Timber.d("AlarmScheduler: Scheduling Calendar trigger alarm for routine $routineId, trigger ${trigger.uuid}")
        // Similar a scheduleTimeTriggerAlarm, pero parseando los datos específicos del trigger de calendario.
        // Esto podría incluir fechas, rangos de fechas, etc.

        val triggerData = trigger.data.data
        // TODO: Parsear datos de trigger de calendario (fecha, hora, recurrencia, etc.)

        // Ejemplo (simplificado): Programar para una fecha y hora específica
        val year = triggerData["year"] as? Int ?: return
        val month = triggerData["month"] as? Int ?: return // Calendar.MONTH es base 0
        val dayOfMonth = triggerData["dayOfMonth"] as? Int ?: return
        val hour = triggerData["hour"] as? Int ?: return
        val minute = triggerData["minute"] as? Int ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarmTime = calendar.timeInMillis

        // Asegúrate de que la fecha de la alarma no esté en el pasado (a menos que quieras que se dispare inmediatamente si se programa tarde)
        if (alarmTime <= System.currentTimeMillis()) {
            Timber.w("AlarmScheduler: Calendar trigger date is in the past for trigger ${trigger.uuid}. Not scheduling.")
            return // No programar si la fecha ya pasó
        }


        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ROUTINE_ID", routineId)
            putExtra("TRIGGER_UUID", trigger.uuid.toString())
            // Añade otros datos relevantes
        }

        val requestCode = (routineId.toInt() * 1000 + trigger.uuid.hashCode()).absoluteValue
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
            Timber.d("AlarmScheduler: Exact alarm scheduled for Calendar trigger ${trigger.uuid} at ${Date(alarmTime)}")
        } else {
            Timber.e("AlarmScheduler: Cannot schedule exact alarm for Calendar trigger ${trigger.uuid}: Permission missing.")
            // Considerar notificar al usuario
        }
    }


    /**
     * Cancels all alarms associated with a specific routine.
     * This is important when a routine is updated or deleted.
     * @param routineId The ID of the routine whose alarms should be cancelled.
     */
    fun cancelRoutineAlarms(routineId: Long) {
        Timber.d("AlarmScheduler: Cancelling alarms for routine ID: $routineId")
        // Aquí necesitarás una forma de saber qué PendingIntents existen para esta rutina
        // para poder cancelarlos. Esto puede ser un poco complejo.

        // Una estrategia común es reconstruir el PendingIntent usando el mismo request code
        // y el mismo Intent utilizado para programar la alarma, y luego llamar a cancel().

        // **Importante:** Para cancelar un PendingIntent, debes recrearlo EXACTAMENTE
        // con el mismo contexto, request code y Intent (incluyendo extras).

        // Si cada trigger tiene un UUID único y utilizas una combinación de routineId y
        // trigger UUID para el request code (como en el ejemplo scheduleTimeTriggerAlarm),
        // necesitarás acceder a los triggers de la rutina para recrear los PendingIntents.

        // Esto implicaría leer la rutina y sus triggers de la base de datos.

        // Ejemplo (Conceptual - necesitas adaptar esto a tu acceso a datos):
        /*
        val routine = routineRepository.getRoutineById(routineId) // Necesitas una forma de obtener la rutina
        routine?.triggers?.forEach { trigger ->
             val requestCode = (routineId.toInt() * 1000 + trigger.uuid.hashCode()).absoluteValue
             val intent = Intent(context, AlarmReceiver::class.java).apply {
                 putExtra("ROUTINE_ID", routineId)
                 putExtra("TRIGGER_UUID", trigger.uuid.toString())
                 // Asegúrate de añadir los mismos extras que usaste al programar
             }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE // Usar FLAG_NO_CREATE para no crear el PendingIntent si no existe
            )

            pendingIntent?.cancel()
            Timber.d("AlarmScheduler: Cancelled alarm for trigger ${trigger.uuid} (Request Code: $requestCode)")
        }
        */

        // TODO: Implementar lógica robusta para cancelar alarmas,
        //  posiblemente leyendo los triggers de la rutina para reconstruir los PendingIntents.
        Timber.w("AlarmScheduler: cancelRoutineAlarms() is a TODO. Alarms might not be cancelled correctly on routine update/delete.")

    }


    // TODO: Implementar un BroadcastReceiver (AlarmReceiver) que maneje los Intents de las alarmas.
    //  Este receiver será el punto de entrada cuando una alarma se dispare.
    //  Dentro del receiver, obtendrás el ID de la rutina y el UUID del trigger del Intent,
    //  cargarás la rutina y sus acciones, y ejecutarás las acciones.
}
