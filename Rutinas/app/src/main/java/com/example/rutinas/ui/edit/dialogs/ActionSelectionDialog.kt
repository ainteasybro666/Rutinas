package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.DialogActionSelectionBinding
import timber.log.Timber

class ActionSelectionDialog : DialogFragment() {
    interface ActionSelectionListener {
        fun onActionSelected(action: Action)
    }

    private var _binding: DialogActionSelectionBinding? = null
    private val binding get() = _binding!!
    private var actionSelectionListener: ActionSelectionListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("ActionSelectionDialog: onCreateDialog() llamado")
        _binding = DialogActionSelectionBinding.inflate(LayoutInflater.from(context))

        val builder = AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
            setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
        }

        binding.btnSelectAction.setOnClickListener {
            val selectedActionType = when (binding.rgActionTypes.checkedRadioButtonId) {
                binding.rbAlarm.id -> ActionType.ALARM
                binding.rbAnnouncement.id -> ActionType.ANNOUNCEMENT
                binding.rbBrightness.id -> ActionType.BRIGHTNESS
                binding.rbVolume.id -> ActionType.VOLUME
                binding.rbSoundMode.id -> ActionType.SOUND_MODE
                binding.rbTime.id -> ActionType.TIME
                binding.rbReadNotifications.id -> ActionType.READ_NOTIFICATIONS
                binding.rbPause.id -> ActionType.PAUSE
                else -> null
            }

            selectedActionType?.let {
                val action = Action(type = it, data = DataWrapper(emptyMap()))
                actionSelectionListener?.onActionSelected(action)
            }

            dismiss()
        }
        return builder.create()
    }


    fun setActionSelectionListener(listener: ActionSelectionListener) {
        this.actionSelectionListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ActionSelectionDialog {
            return ActionSelectionDialog()
        }
    }
}
