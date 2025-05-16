package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditVolumeActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class EditVolumeActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditVolumeActionBinding>(listener) {

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditVolumeActionDialogFragment {
            return EditVolumeActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditVolumeActionBinding {
        Timber.d("EditVolumeActionDialogFragment: Inflating binding")
        return FragmentEditVolumeActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditVolumeActionDialogFragment: onViewCreated() called")
        setupUI()
        loadActionData(actionToEdit) // Usar actionToEdit
    }

    // Override onCreateDialog to set up the dialog structure
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditVolumeActionDialogFragment: onCreateDialog() called.")
        val dialog = super.onCreateDialog(savedInstanceState) as AlertDialog

        // The UI setup (listeners and initial state) will be done in onViewCreated

        return dialog
    }

    private fun setupUI() {
        with(binding) {
            // Setup Listeners for CheckBoxes and SeekBars
            cbVolumeMedia.setOnCheckedChangeListener { _, isChecked ->
                seekBarMediaVolume.visibility = if (isChecked) View.VISIBLE else View.GONE
                tvMediaVolumeValue.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            seekBarMediaVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvMediaVolumeValue.text = progress.toString() // Update TextView with current progress
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            cbVolumeRingtone.setOnCheckedChangeListener { _, isChecked ->
                seekBarRingtoneVolume.visibility = if (isChecked) View.VISIBLE else View.GONE
                tvRingtoneVolumeValue.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            seekBarRingtoneVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvRingtoneVolumeValue.text = progress.toString() // Update TextView with current progress
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            cbVolumeAlarm.setOnCheckedChangeListener { _, isChecked ->
                seekBarAlarmVolume.visibility = if (isChecked) View.VISIBLE else View.GONE
                tvAlarmVolumeValue.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            seekBarAlarmVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvAlarmVolumeValue.text = progress.toString() // Update TextView with current progress
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Add listeners for Save and Cancel buttons
            btnSaveVolumeAction.setOnClickListener {
                Timber.d("EditVolumeActionDialogFragment: Save button clicked.")
//                onSaveAction()
            }

            btnCancelVolumeAction.setOnClickListener {
                Timber.d("EditVolumeActionDialogFragment: Cancel button clicked.")
                dismiss()
            }
        }
    }

    override fun loadActionData(action: Action?) {
        Timber.d("EditVolumeActionDialogFragment: loadActionData() called")
        try {
            action?.data?.let { dataWrapper -> // Usar action?.data
                dataWrapper.data.forEach { (key, value) ->
                    when (key) {
                        "mediaVolume" -> {
                            binding.cbVolumeMedia.isChecked = true
                            binding.seekBarMediaVolume.progress = (value as? Int) ?: 50
                            binding.tvMediaVolumeValue.text = (value as? Int)?.toString() ?: "50"
                            binding.seekBarMediaVolume.visibility = View.VISIBLE
                            binding.tvMediaVolumeValue.visibility = View.VISIBLE
                        }
                        "ringtoneVolume" -> {
                            binding.cbVolumeRingtone.isChecked = true
                            binding.seekBarRingtoneVolume.progress = (value as? Int) ?: 50
                            binding.tvRingtoneVolumeValue.text = (value as? Int)?.toString() ?: "50"
                            binding.seekBarRingtoneVolume.visibility = View.VISIBLE
                            binding.tvRingtoneVolumeValue.visibility = View.VISIBLE
                        }
                        "alarmVolume" -> {
                            binding.cbVolumeAlarm.isChecked = true
                            binding.seekBarAlarmVolume.progress = (value as? Int) ?: 50
                            binding.tvAlarmVolumeValue.text = (value as? Int)?.toString() ?: "50"
                            binding.seekBarAlarmVolume.visibility = View.VISIBLE
                            binding.tvAlarmVolumeValue.visibility = View.VISIBLE
                        }
                    }
                }
            }
            Timber.d("EditVolumeActionDialogFragment: Action data loaded into UI.")
        } catch (e: Exception) {
            Timber.e("EditVolumeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveActionData(): Action {
        Timber.d("EditVolumeActionDialogFragment: saveActionData() called")
        try {
            val volumeData = mutableMapOf<String, Any>()

            if (binding.cbVolumeMedia.isChecked) {
                volumeData["mediaVolume"] = binding.seekBarMediaVolume.progress
            }

            if (binding.cbVolumeRingtone.isChecked) {
                volumeData["ringtoneVolume"] = binding.seekBarRingtoneVolume.progress
            }

            if (binding.cbVolumeAlarm.isChecked) {
                volumeData["alarmVolume"] = binding.seekBarAlarmVolume.progress
            }

            // Create DataWrapper with collected data
            val data = DataWrapper(volumeData)

            // Return the updated Action object
            val updatedAction = actionToEdit.copy( // Usar actionToEdit
                actionType = ActionType.VOLUME,
                data = data,
                // Keep existing routineId, executionOrder, pauseDuration
                routineId = actionToEdit.routineId,
                executionOrder = actionToEdit.executionOrder,
                pauseDuration = actionToEdit.pauseDuration
            )

            Timber.d("EditVolumeActionDialogFragment: Acción actualizada y notificada")
            return updatedAction
        } catch (e: Exception) {
            Timber.e("EditVolumeActionDialogFragment: Error al guardar acción - ${e.message}")
            throw e // Relanzar la excepción
        }
    }

    // Implementar onSaveAction
//    override fun onSaveAction() { // Make this function public or protected if needed from base
//        Timber.d("EditVolumeActionDialogFragment: onSaveAction() llamado")
//        try {
//            val updatedAction = saveActionData()
//            notifyActionUpdated(updatedAction)
//            Timber.d("EditVolumeActionDialogFragment: Action updated and notified")
//
//            // Log the saved data for verification
//            Timber.d("EditVolumeActionDialogFragment: Saved Data: ${updatedAction.data?.data}")
//            dismiss()
//        } catch (e: Exception) {
//            Timber.e("EditVolumeActionDialogFragment: Error saving action - ${e.message}")
//            showErrorDialog("Error al guardar la acción: ${e.message}")
//        }
//    }
}