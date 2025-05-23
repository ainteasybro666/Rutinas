package com.example.rutinas.domain.usecase

import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import javax.inject.Inject

class DeleteRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler // Inyecta el scheduler aquí
) {
    suspend operator fun invoke(routine: Routine) {
        // Cancelar alarmas usando el scheduler
        alarmScheduler.cancel(routine)

        // Eliminar del repositorio (base de datos)
        routineRepository.deleteRoutine(routine)
    }
}