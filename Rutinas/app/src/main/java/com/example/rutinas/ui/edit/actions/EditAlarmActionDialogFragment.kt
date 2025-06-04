package com.example.rutinas.ui.edit.actions

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAlarmActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class EditAlarmActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditAlarmActionBinding>(listener) {

    private var selectedAlarmSoundUri: Uri? = null
    // TODO: Alarm Action: Implement a proper vibration pattern selection/creation UI.
    // For now, we'll rely on a simple stored pattern or the system default if none is specified or loaded.
    private var selectedVibrationPattern: LongArray? = null // To store a custom or selected pattern

    // ActivityResultLauncher for Alarm Sound Picker
    private val alarmSoundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                selectedAlarmSoundUri = it
                // Display the selected sound's title (or URI if title not easily available)
                val ringtone = RingtoneManager.getRingtone(context, it)
                val title = ringtone.getTitle(context)
                binding.actvAlarmSound.setText(title, false)
            }
        }
    }

    // We remove the explicit vibrationPatternPickerLauncher since we're not using a system picker now.
    // A custom UI will be needed later if we implement pattern selection.

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditAlarmActionDialogFragment {
            return EditAlarmActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditAlarmActionBinding {
        Timber.d("EditAlarmActionDialogFragment: Inflating binding")
        return FragmentEditAlarmActionBinding.inflate(inflater, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditAlarmActionDialogFragment: onCreateDialog() called.")
        val dialog = super.onCreateDialog(savedInstanceState) as AlertDialog

        setupUI()
        loadActionData(actionToEdit)

        return dialog
    }

    private fun setupUI() {
        with(binding) {
            // Setup Spinners for Duration and Repetitions
            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.duration_presets,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                etDuration.adapter = adapter
            }

            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.repetition_counts,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                etRepetitions.adapter = adapter
            }

            // Setup Click Listeners for Sound and Vibration pickers
            actvAlarmSound.setOnClickListener {
                openAlarmSoundPicker()
            }

            // TODO: Alarm Action: Replace this click listener with opening a custom vibration pattern selection/creation UI.
            actvVibrationPattern.setOnClickListener {
                Timber.d("Vibration Pattern field clicked. Placeholder for custom vibration pattern selection UI.")
                // For now, we'll just display a message or a simple indicator
                binding.actvVibrationPattern.setText("Vibración por defecto", false) // Indicate using default
                selectedVibrationPattern = null // Ensure no custom pattern is saved for now
            }
        }
    }

    private fun openAlarmSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedAlarmSoundUri)
        alarmSoundPickerLauncher.launch(intent)
    }


    override fun loadActionData(action: Action?) {
        Timber.d("EditAlarmActionDialogFragment: Loading action data: $action")
        action?.data?.data?.let { dataMap ->
            // Load Action Label
            binding.etAlarmActionLabel.setText(dataMap["actionLabel"]?.toString())

            // Load Alarm Sound
            dataMap["alarmSoundUri"]?.toString()?.let { uriString ->
                selectedAlarmSoundUri = Uri.parse(uriString)
                val ringtone = RingtoneManager.getRingtone(context, selectedAlarmSoundUri)
                val title = ringtone.getTitle(context)
                binding.actvAlarmSound.setText(title, false)
            } ?: run {
                // Load default alarm sound if none is saved
                selectedAlarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val ringtone = RingtoneManager.getRingtone(context, selectedAlarmSoundUri)
                val title = ringtone.getTitle(context)
                binding.actvAlarmSound.setText(title, false)
            }

            // TODO: Alarm Action: Properly load and display the saved vibration pattern when the custom UI is implemented.
            // For now, we'll just check if a pattern was saved and indicate it.
            dataMap["vibrationPattern"]?.let {
                Timber.d("EditAlarmActionDialogFragment: Saved vibration pattern found, but not loaded/displayed by current UI.")
                binding.actvVibrationPattern.setText("Patrón guardado (no visualizable)", false) // Indicate a pattern was saved
                // Attempt to parse if you still want to keep the internal representation
                try {
                    val patternString = it.toString().removePrefix("[").removeSuffix("]")
                    selectedVibrationPattern = patternString.split(",").map { value -> value.trim().toLong() }.toLongArray()
                } catch (e: Exception) {
                    Timber.e("Error parsing saved vibration pattern: ${e.message}")
                    selectedVibrationPattern = null
                }
            } ?: run {
                binding.actvVibrationPattern.setText("Vibración por defecto", false) // Indicate using default
                selectedVibrationPattern = null // Clear any selected custom pattern
            }


            // Load Duration
            val duration = dataMap["duration"]?.toString()
            val durationPresets = resources.getStringArray(R.array.duration_presets)
            val durationPosition = durationPresets.indexOf(duration)
            if (durationPosition != -1) {
                binding.etDuration.setSelection(durationPosition)
            }

            // Load Repetitions
            val repetitions = dataMap["repetitions"]?.toString()
            val repetitionCounts = resources.getStringArray(R.array.repetition_counts)
            val repetitionsPosition = repetitionCounts.indexOf(repetitions)
            if (repetitionsPosition != -1) {
                binding.etRepetitions.setSelection(repetitionsPosition)
            }

            // Load Stop on Tap
            binding.switchStopOnTap.isChecked = dataMap["stopOnTap"]?.toString().toBoolean()

            // Load Only Vibration
            binding.switchOnlyVibration.isChecked = dataMap["onlyVibration"]?.toString().toBoolean()

            // Load Ignore DND
            binding.switchIgnoreDnd.isChecked = dataMap["ignoreDnd"]?.toString().toBoolean()


            Timber.d("EditAlarmActionDialogFragment: Action data loaded into UI.")
        } ?: Timber.d("EditAlarmActionDialogFragment: No action data to load (creating new action).")
    }

    override fun saveActionData(): Action {
        Timber.d("EditAlarmActionDialogFragment: Saving action data.")
        // Collect data from the views
        val actionLabel = binding.etAlarmActionLabel.text.toString()
        val alarmSoundUriString = selectedAlarmSoundUri?.toString()
        // TODO: Alarm Action: Save the selected vibration pattern properly when the custom UI is implemented.
        // For now, we will not save the selectedVibrationPattern from this UI, relying on the default in RoutineExecutor
        val vibrationPatternString: String? = null // We are not saving a pattern from this UI for now

        val stopOnTap = binding.switchStopOnTap.isChecked
        val onlyVibration = binding.switchOnlyVibration.isChecked
        val ignoreDnd = binding.switchIgnoreDnd.isChecked


        // Get selected values from Spinners
        val duration = binding.etDuration.selectedItem?.toString()
        val repetitions = binding.etRepetitions.selectedItem?.toString()

        // Create DataWrapper
        val data = DataWrapper(
            mutableMapOf<String, Any>().apply {
                if (actionLabel.isNotBlank()) {
                    this["actionLabel"] = actionLabel
                }
                if (alarmSoundUriString != null) {
                    this["alarmSoundUri"] = alarmSoundUriString
                }
                // TODO: Alarm Action: Include vibrationPatternString here when the custom UI is implemented and saving is desired.
                // if (vibrationPatternString != null) {
                //    this["vibrationPattern"] = vibrationPatternString // Save as String
                // }

                this["stopOnTap"] = stopOnTap.toString()
                this["onlyVibration"] = onlyVibration.toString()
                this["ignoreDnd"] = ignoreDnd.toString()
                if (duration != null) {
                    this["duration"] = duration
                }
                if (repetitions != null) {
                    this["repetitions"] = repetitions
                }
            }
        )

        // Return the Action object (UUID will be handled in the ViewModel/Fragment)
        return actionToEdit.copy( // Use actionToEdit for updates
            actionType = ActionType.ALARM,
            data = data,
            // Keep existing routineId, executionOrder, pauseDuration
            routineId = actionToEdit.routineId,
            executionOrder = actionToEdit.executionOrder,
            pauseDuration = actionToEdit.pauseDuration
        )
    }
}