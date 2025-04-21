package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.ui.edit.actions.EditAlarmActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditAnnouncementActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditBrightnessActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditPauseActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditSoundModeActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditTimeActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditVolumeActionDialogFragment

import com.example.rutinas.ui.edit.actions.ActionEditorDialog




class EditActionDialog : DialogFragment() {
    private var _binding: DialogEditActionBinding? = null
    private val binding get() = _binding!!
    private var onActionUpdatedListener: ((Action) -> Unit)? = null
    private lateinit var action: Action

    fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        action = arguments?.getParcelable<Action>(ARG_ACTION) as? Action
            ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

        val dialog = when (action.type) {
            ActionType.ALARM -> EditAlarmActionDialogFragment.newInstance(action)
            ActionType.ANNOUNCEMENT -> EditAnnouncementActionDialogFragment.newInstance(action)
            ActionType.BRIGHTNESS -> EditBrightnessActionDialogFragment.newInstance(action)
            ActionType.VOLUME -> EditVolumeActionDialogFragment.newInstance(action)
            ActionType.SOUND_MODE -> EditSoundModeActionDialogFragment.newInstance(action)
            ActionType.TIME -> EditTimeActionDialogFragment.newInstance(action)
            ActionType.PAUSE -> EditPauseActionDialogFragment.newInstance(action)
            else -> throw IllegalArgumentException("Tipo de acción no soportado: ${action.type}")
        }

        dialog.setOnActionUpdatedListener { updatedAction ->
            onActionUpdatedListener?.invoke(updatedAction)
        }

        dialog.show(childFragmentManager, "EditActionDialogFragment")
        dismiss()
        return Dialog(requireContext()) // Dummy dialog, will be dismissed immediately
    }


    private fun getUpdatedAction(): Action {
        return when (action.type) {

            else -> action
        }
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action) = EditActionDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_ACTION, action)
            }
        }
    }
}