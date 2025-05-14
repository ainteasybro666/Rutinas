package com.example.rutinas.routines.execution

import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType

// Interface to listen for events during routine execution.
// This allows the RoutineExecutor to communicate back to the component
// that started the execution (e.g., a Service or ViewModel).
interface RoutineExecutionListener {

    /**
     * Called when the routine execution starts.
     * @param routineId The ID of the routine that started executing.
     */
    fun onRoutineExecutionStart(routineId: Long)

    /**
     * Called when an individual action within the routine starts executing.
     * @param action The action that is starting.
     */
    fun onActionStarted(action: Action)

    /**
     * Called when an individual action finishes execution.
     * @param actionType The type of action that finished.
     * @param success True if the action completed successfully, false otherwise.
     */
    fun onActionFinished(actionType: ActionType, success: Boolean)

    /**
     * Called when the entire routine execution finishes.
     * @param routineId The ID of the routine that finished executing.
     * @param success True if all actions completed successfully, false otherwise.
     */
    fun onRoutineExecutionFinished(routineId: Long, success: Boolean)

    /**
     * Called when the routine execution is cancelled.
     * This might happen due to a user action or an error.
     * @param routineId The ID of the routine that was cancelled.
     */
    fun onRoutineExecutionCancelled(routineId: Long)

    // You might add other methods here, e.g., onProgressUpdate, onError, etc.

    fun onEnableStopOnTap()
    fun onRemoveStopOnTapOverlay()
}
