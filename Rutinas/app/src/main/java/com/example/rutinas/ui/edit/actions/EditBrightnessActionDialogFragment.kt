package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditBrightnessActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import com.example.rutinas.ui.edit.actions.BaseEditActionDialogFragment.Companion.newBundle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class EditBrightnessActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditBrightnessActionBinding>(listener) {

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditBrightnessActionDialogFragment {
            return EditBrightnessActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditBrightnessActionBinding {
        Timber.d("EditBrightnessActionDialogFragment: Inflating binding")
        return FragmentEditBrightnessActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditBrightnessActionDialogFragment: onViewCreated() llamado")
        loadActionData(actionToEdit) // Usar actionToEdit
    }

    override fun loadActionData(action: Action?) { // Implementar método abstracto
        Timber.d("EditBrightnessActionDialogFragment: loadActionData() llamado")
        try {
            action?.data?.let { dataWrapper -> // Usar action?.data
                dataWrapper.data.let { data -> // Usar dataWrapper
                    val value = data["value"] as? Int ?: 100
                    val automatic = data["automatic"] as? Boolean ?: false
                    binding.sbBrightness.progress = value
                    binding.switchAutomatic.isChecked = automatic
                }
            }
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveActionData(): Action { // Implementar método abstracto
        Timber.d("EditBrightnessActionDialogFragment: saveActionData() llamado")
        try {
            val value = binding.sbBrightness.progress
            val automatic = binding.switchAutomatic.isChecked
            val updatedAction = actionToEdit.copy( // Usar actionToEdit
                actionType = ActionType.BRIGHTNESS,
                data = DataWrapper(
                    mapOf(
                        "value" to value,
                        "automatic" to automatic
                    ) as MutableMap<String, Any> // Añadir cast
                )
            )
            return updatedAction
            Timber.d("EditBrightnessActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error al guardar acción - ${e.message}")
            throw e // Relanzar la excepción
        }
    }

    // Implementar onSaveAction
    override fun onSaveAction() {
        Timber.d("EditBrightnessActionDialogFragment: onSaveAction() llamado")
        try {
            val updatedAction = saveActionData()
            notifyActionUpdated(updatedAction)
            Timber.d("EditBrightnessActionDialogFragment: Action updated and notified")
            dismiss()
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error saving action - ${e.message}")
            showErrorDialog("Error al guardar la acción: ${e.message}")
        }
    }
}
