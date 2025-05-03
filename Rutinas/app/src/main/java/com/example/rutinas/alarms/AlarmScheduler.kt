package com.example.rutinas.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.rutinas.data.model.RoutineEntity
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.receivers.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val routineRepository: RoutineRepository // Inject RoutineRepository here
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(routine: Routine) {
        Timber.d("Programando alarmas para la rutina: ${routine.uuid}, ${routine.name}")

        // Cancelar alarmas existentes antes de programar nuevas
        cancel(routine)

        routine.triggers.filter { it.triggerType == "TIME" }
            .forEach { trigger -> scheduleTimeTrigger(routine, trigger) }
    }

    private fun scheduleTimeTrigger(routine: Routine, trigger: Trigger) {
        //Comprobamos que el dataWrapper no sea nulo.
        val dataWrapper = trigger.data ?: return
        //Comprobamos que el mapa no sea nulo
        val mapData = dataWrapper.data ?: return

        val hour = mapData["hour"] as? Int
        val minute = mapData["minute"] as? Int
        val frequency = mapData["frequency"] as? String ?: "daily"

        if (hour == null || minute == null) {
            throw IllegalArgumentException("Hour and minute must be specified for time triggers.")
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Primero: ajustar para ocurrencias pasadas
            when (frequency) {
                "daily" -> if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
                "weekly" -> {
                    val daysOfWeek = mapData["daysOfWeek"] as? List<Int>
                    if (daysOfWeek.isNullOrEmpty()) return@apply
                    while (timeInMillis <= System.currentTimeMillis() ||
                        !daysOfWeek.contains(get(Calendar.DAY_OF_WEEK) - 1)
                    ) {
                        add(Calendar.DAY_OF_WEEK, 1)
                    }
                }
                "monthly" -> if (timeInMillis <= System.currentTimeMillis()) add(Calendar.MONTH, 1)
            }

            // Segundo: ajustes adicionales según frecuencia
            when (frequency) {
                "weekly" -> {
                    val daysOfWeek = mapData["daysOfWeek"] as? List<Int> ?: return@apply
                    val target = (daysOfWeek.minOrNull() ?: return@apply) + 1
                    while (get(Calendar.DAY_OF_WEEK) != target) add(Calendar.DAY_OF_WEEK, 1)
                }
                "monthly" -> {
                    val dayOfMonth = (mapData["dayOfMonth"] as? Number)?.toInt() ?: 1
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    if (get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                }
            }
        }

        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)

        // Log usando la variable 'frequency' declarada fuera del apply
        Timber.d("Programando alarma $frequency para $hour:$minute (${routine.name}), id: $alarmId, time: ${calendar.time}")

        if (canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 300_000, pendingIntent)
            Timber.w("Sin permisos para alarmas exactas; usando ventana de 5 minutos.")
        }
    }

    fun cancel(routine: Routine) {
        Timber.d("Cancelando alarmas para la rutina: ${routine.uuid}, ${routine.name}")
        routine.triggers.filter { it.triggerType == "TIME" }.forEach { trigger ->
            cancelTimeTrigger(routine, trigger)
        }
    }

    private fun cancelTimeTrigger(routine: Routine, trigger: Trigger) {
        val alarmId = generateAlarmId(routine, trigger)
        val pendingIntent = createPendingIntent(routine, alarmId)
        alarmManager.cancel(pendingIntent)
        Timber.d("Alarma cancelada para ${routine.name}, id: $alarmId")
    }

    private fun generateAlarmId(routine: Routine, trigger: Trigger): Int {
        // Combine routine UUID and a trigger identifier to create a unique ID
        return "${routine.uuid}-${trigger.uuid}".hashCode()
    }

    // Now this method uses RoutineRepository to get the RoutineEntity
    private suspend fun getRoutineEntityByUuid(uuid: String): RoutineEntity? {
        Timber.d("Buscando rutina en la base de datos con UUID: $uuid")
        val routine = routineRepository.getRoutineByUuid(uuid)
        return routine?.let { // Convertir de Routine a RoutineEntity
            RoutineEntity(
                id = it.id,
                uuid = it.uuid,
                name = it.name,
                description = it.description,
                isEnabled = it.isEnabled,
                createdDate = it.createdDate
            )
        }
    }

    private fun createPendingIntent(routine: Routine, alarmId: Int): PendingIntent {
        //  Obtener la RoutineEntity a partir del UUID.  Adaptar esto según tu implementación.
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_UUID, routine.uuid)  // Pasar UUID
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
            true // En versiones anteriores a Android 12, no hay restricciones
        }
    }
}
