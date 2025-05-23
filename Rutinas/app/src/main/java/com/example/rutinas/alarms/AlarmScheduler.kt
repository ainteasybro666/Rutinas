package com.example.rutinas.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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
        Timber.d("AlarmScheduler: Cancelling single alarm for routine UUID: $routineUuid, trigger UUID: $triggerUuid")
        // TODO: Implement actual cancellation logic.
        //  This will involve recreating the PendingIntent using the same logic
        //  as when scheduling, and then calling pendingIntent.cancel()
        val alarmId = generateAlarmId(routineUuid, triggerUuid) // Need a generateAlarmId function that takes UUIDs
        val pendingIntent = createPendingIntentForCancellation(routineUuid, alarmId) // Need a dedicated function for cancellation PendingIntent

        pendingIntent?.cancel()
        Timber.d("AlarmScheduler: Attempted to cancel alarm with ID: $alarmId")
    }

    // Función para cancelar todas las alarmas de una rutina.
    // Signature based on the service calling alarmScheduler.cancelRoutineAlarms(routine)
    // Accepts Routine object because the service passes it and it's likely needed to get triggers
    suspend fun cancelRoutineAlarms(routine: Routine) { // Made suspend as it might need to read triggers from DB
        Timber.d("AlarmScheduler: Cancelling alarms for routine: ${routine.name} (${routine.uuid})")
        // TODO: Implement actual cancellation logic.
        //  This will involve fetching triggers for the routine (potentially suspend operation via repository)
        //  and then cancelling each one using logic similar to cancelSingleAlarm.

        // Example conceptual implementation:
        val triggers = routineRepository.getTriggersForRoutine(routine.id) // Assuming you can get triggers this way

        triggers.forEach { trigger ->
            cancelSingleAlarm(routine.uuid, trigger.uuid.toString()) // Use the single cancellation logic
        }

        Timber.d("AlarmScheduler: Finished attempting to cancel alarms for routine: ${routine.uuid}")
    }

    // Función para cancelar una alarma específica por UUID de rutina y Alarm ID
    // Signature based on the service calling cancelSingleAlarmByInfo(routineUuid, alarmId)
    fun cancelSingleAlarmByInfo(routineUuid: String, alarmId: Int) {
        Timber.d("AlarmScheduler: Cancelling single alarm by info for routine UUID: $routineUuid, alarm ID: $alarmId")
        // TODO: Implement actual cancellation logic.
        //  This is similar to cancelSingleAlarm, but you already have the alarmId.
        val pendingIntent = createPendingIntentForCancellation(routineUuid, alarmId)

        pendingIntent?.cancel()
        Timber.d("AlarmScheduler: Attempted to cancel alarm with ID: $alarmId")
    }
    
    // <<-- NUEVO: Función genérica para cancelar cualquier tipo de trigger -->>
    private fun cancelTrigger(routine: Routine, trigger: Trigger) {
        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)
        alarmManager.cancel(pendingIntent)
        Timber.d("Alarma cancelada para ${routine.name}, trigger id: $alarmId")
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

    private fun createPendingIntentForCancellation(routineUuid: String, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_UUID, routineUuid) // Usar el UUID pasado como String
            // <<-- ¡IMPORTANTE! Si descomentaste y usas putExtra("TRIGGER_UUID", trigger.uuid) en createPendingIntent,
            // debes añadirlo aquí también, pero necesitarás el triggerUuid como parámetro de esta función!
            // Si no pasas triggerUuid a esta función, no podrás recrear el Intent exacto si incluyes ese extra.
            // Esto afectaría a cancelSingleAlarmByInfo si necesita ese extra.
            // Si triggerUuid SÓLO se usa para generar el alarmId y NO como extra en el Intent, entonces está bien.
            // Verifica si descomentaste o usas putExtra("TRIGGER_UUID", ...) en createPendingIntent.
        }

        // Use FLAG_NO_CREATE para buscar el PendingIntent existente
        // Usa los mismos flags adicionales que en createPendingIntent
        return PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
                "TIME" -> {
                    // 3, 4, 5: Programar trigger de tiempo
                    // Llama a tu función existente o crea una nueva si es necesario
                    // scheduleTimeTrigger(routine, trigger)
                    // Tu AlarmReceiver sugiere que ya tienes una función scheduleTimeTrigger
                    Timber.d("AlarmScheduler: Scheduling Time trigger: ${trigger.uuid}")
                    scheduleTimeTrigger(routine, trigger) // Llama a tu método existente
                }
                "CALENDAR" -> {
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
