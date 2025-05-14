package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditSoundModeActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint // Asegurarse que tiene la anotación
class EditSoundModeActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditSoundModeActionBinding>(listener) {

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditSoundModeActionDialogFragment {
            return EditSoundModeActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditSoundModeActionBinding {
        Timber.d("EditSoundModeActionDialogFragment: Inflating binding")
        return FragmentEditSoundModeActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditSoundModeActionDialogFragment: onViewCreated() llamado")
        loadActionData(actionToEdit) // Usar actionToEdit
    }

    override fun loadActionData(action: Action?) { // Implementar método abstracto
        Timber.d("EditSoundModeActionDialogFragment: loadActionData() llamado")
        try {
            action?.data?.let { dataWrapper -> // Usar action?.data
                dataWrapper.data.let { data -> // Usar dataWrapper
                    val value = data["value"] as? String ?: "NORMAL"
                    when (value) {
                        "NORMAL" -> binding.rbNormal.isChecked = true
                        "SILENT" -> binding.rbSilent.isChecked = true
                        "VIBRATE" -> binding.rbVibrate.isChecked = true
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("EditSoundModeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveActionData(): Action { // Implementar método abstracto
        Timber.d("EditSoundModeActionDialogFragment: saveActionData() llamado")
        try {
            val value = when (binding.radioGroupSoundMode.checkedRadioButtonId) {
                binding.rbNormal.id -> "NORMAL"
                binding.rbSilent.id -> "SILENT"
                binding.rbVibrate.id -> "VIBRATE"
                else -> "NORMAL"
            }

            val updatedAction = actionToEdit.copy( // Usar actionToEdit
                actionType = ActionType.SOUND_MODE,
                data = DataWrapper(
                    mapOf(
                        "value" to value
                    ) as MutableMap<String, Any> // Añadir cast
                )
            )
            return updatedAction
            Timber.d("EditSoundModeActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditSoundModeActionDialogFragment: Error al guardar acción - ${e.message}")
            throw e // Relanzar la excepción
        }
    }

    // Implementar onSaveAction
    override fun onSaveAction() {
        Timber.d("EditSoundModeActionDialogFragment: onSaveAction() llamado")
        try {
            val updatedAction = saveActionData()
            notifyActionUpdated(updatedAction)
            Timber.d("EditSoundModeActionDialogFragment: Action updated and notified")
            dismiss()
        } catch (e: Exception) {
            Timber.e("EditSoundModeActionDialogFragment: Error saving action - ${e.message}")
            showErrorDialog("Error al guardar la acción: ${e.message}")
        }
    }
}
