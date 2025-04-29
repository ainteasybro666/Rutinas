package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.DialogAddActionBinding
import timber.log.Timber

class AddActionDialogFragment : DialogFragment() {
    interface ActionSelectedListener {
        fun onActionSelected(action: Action)
    }

    private var _binding: DialogAddActionBinding? = null
    private val binding get() = _binding!!
    private var actionSelectedListener: ActionSelectedListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddActionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Aceptar") { _, _ -> saveAction() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
        return dialog
    }

    private fun saveAction() {
        Timber.d("AddActionDialogFragment: saveAction() llamado")
        val selectedActionType = when (binding.rgActionTypes.checkedRadioButtonId) {
            binding.rbAlarm.id -> ActionType.ALARM
            binding.rbAnnouncement.id -> ActionType.ANNOUNCEMENT
            binding.rbBrightness.id -> ActionType.BRIGHTNESS
            binding.rbVolume.id -> ActionType.VOLUME
            binding.rbSoundMode.id -> ActionType.SOUND_MODE
            binding.rbTime.id -> ActionType.TIME
            binding.rbReadNotifications.id -> ActionType.READ_NOTIFICATIONS
            binding.rbPause.id -> ActionType.PAUSE
            else -> throw IllegalStateException("Ningún tipo de acción seleccionado")
        }
        val action = Action(type = selectedActionType, data = DataWrapper(emptyMap()))
        actionSelectedListener?.onActionSelected(action)
    }

    fun setOnActionSelectedListener(listener: (Action) -> Unit) {
        actionSelectedListener = object : ActionSelectedListener {
            override fun onActionSelected(action: Action) {
                listener(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AddActionDialogFragment {
            return AddActionDialogFragment()
        }
    }
}
