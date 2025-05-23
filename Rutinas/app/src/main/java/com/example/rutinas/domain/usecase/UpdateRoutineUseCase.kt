package com.example.rutinas.domain.usecase

import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import javax.inject.Inject

class UpdateRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler // Inyecta el scheduler aquí
) {
    suspend operator fun invoke(routine: Routine) {
        // Cancelar alarmas antiguas
        val oldRoutine = routineRepository.getRoutineByUuid(routine.uuid)
        if (oldRoutine != null) {
            alarmScheduler.cancel(oldRoutine)
        } else {
            // Manejar caso donde la rutina antigua no se encuentra
        }

        // Actualizar en el repositorio
        routineRepository.updateRoutine(routine)

        // Programar nuevas alarmas si está habilitada
        if (routine.isEnabled) {
            alarmScheduler.schedule(routine)
        }
    }
}