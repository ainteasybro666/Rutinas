package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditPauseActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

class EditPauseActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditPauseActionBinding>(listener) {

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditPauseActionDialogFragment {
            return EditPauseActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditPauseActionBinding {
        Timber.d("EditPauseActionDialogFragment: Inflating binding")
        return FragmentEditPauseActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditPauseActionDialogFragment: onViewCreated() llamado")
        setupNumberPickers()
        loadActionData(actionToEdit) // Usar actionToEdit
    }

    private fun setupNumberPickers() {
        binding.npHours.apply {
            minValue = 0
            maxValue = 23
        }
        binding.npMinutes.apply {
            minValue = 0
            maxValue = 59
        }
        binding.npSeconds.apply {
            minValue = 0
            maxValue = 59
        }
    }

    override fun loadActionData(action: Action?) { // Implementar método abstracto
        Timber.d("EditPauseActionDialogFragment: loadActionData() llamado")
        try {
            action?.data?.let { dataWrapper -> // Usar action?.data
                dataWrapper.data.let { data -> // Usar dataWrapper
                    val totalSeconds = data["value"] as? Long ?: 0
                    val hours = (totalSeconds / 3600).toInt()
                    val minutes = ((totalSeconds % 3600) / 60).toInt()
                    val seconds = (totalSeconds % 60).toInt()

                    binding.npHours.value = hours
                    binding.npMinutes.value = minutes
                    binding.npSeconds.value = seconds
                }
            }
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveActionData(): Action { // Implementar método abstracto
        Timber.d("EditPauseActionDialogFragment: saveActionData() llamado")
        try {
            val hours = binding.npHours.value
            val minutes = binding.npMinutes.value
            val seconds = binding.npSeconds.value
            val totalSeconds = hours * 3600 + minutes * 60 + seconds

            val updatedAction = actionToEdit.copy( // Usar actionToEdit
                actionType = ActionType.PAUSE,
                data = DataWrapper(
                    mapOf(
                        "value" to totalSeconds
                    ) as MutableMap<String, Any> // Añadir cast
                )
            )
            return updatedAction
            Timber.d("EditPauseActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al guardar acción - ${e.message}")
            throw e // Relanzar la excepción
        }
    }

    // Implementar onSaveAction
    override fun onSaveAction() {
        Timber.d("EditPauseActionDialogFragment: onSaveAction() llamado")
        try {
            val updatedAction = saveActionData()
            notifyActionUpdated(updatedAction)
            Timber.d("EditPauseActionDialogFragment: Action updated and notified")
            dismiss()
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error saving action - ${e.message}")
            showErrorDialog("Error al guardar la acción: ${e.message}")
        }
    }
}
