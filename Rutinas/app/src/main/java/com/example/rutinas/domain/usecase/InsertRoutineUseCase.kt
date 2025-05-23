package com.example.rutinas.domain.usecase

import com.example.rutinas.alarms.AlarmScheduler
import com.example.rutinas.data.repository.RoutineRepository
import com.example.rutinas.domain.Routine
import timber.log.Timber
import javax.inject.Inject

class InsertRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler // Inyecta el scheduler aquí
) {
    suspend operator fun invoke(routine: Routine) {
        // Insertar en el repositorio - SOLO DEVUELVE EL ID
        val routineId = routineRepository.insertRoutine(routine)

        // <<-- NUEVO: Cargar la rutina completa usando el ID -->>
        val insertedRoutine = routineRepository.getRoutineById(routineId) // Usar la función que creaste

        // <<-- NUEVO: Verificar si la rutina fue cargada antes de usarla -->>
        if (insertedRoutine != null) {
            // Ahora insertedRoutine es de tipo Routine y tiene la propiedad isEnabled
            if (insertedRoutine.isEnabled) {
                // Ahora pasas un objeto Routine a schedule()
                alarmScheduler.schedule(insertedRoutine)
            } else {
                // Si no está habilitada, puedes omitir la programación inicial (ya manejado por la condición)
            }
        } else {
            // Manejar el caso donde la rutina recién insertada no pudo ser cargada
            // Esto no debería ocurrir en condiciones normales, pero es bueno manejarlo.
            Timber.e("InsertRoutineUseCase: Could not retrieve routine with ID $routineId after insertion.")
        }
    }
}