package com.example.rutinas.domain.usecase

import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.repository.RoutineRepository
import javax.inject.Inject

class UpdateRoutineStatusUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler // Inyecta el scheduler aquí
) {
    suspend operator fun invoke(routineId: Long, isEnabled: Boolean) {
        // Actualizar estado en el repositorio
        routineRepository.updateRoutineStatus(routineId, isEnabled)

        // Cargar la rutina actualizada para decidir si programar/cancelar
        val routine = routineRepository.getRoutineById(routineId)
        if (routine != null) {
            if (routine.isEnabled) {
                alarmScheduler.schedule(routine)
            } else {
                alarmScheduler.cancel(routine)
            }
        } else {
            // Manejar error si la rutina no se encuentra
        }
    }
}