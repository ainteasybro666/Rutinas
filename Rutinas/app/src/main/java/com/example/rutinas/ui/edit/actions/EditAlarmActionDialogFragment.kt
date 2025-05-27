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
    private var selectedVibrationPattern: LongArray? = null // To store custom or selected pattern

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

    // ActivityResultLauncher for Vibration Pattern Picker (Less common system picker, might need custom implementation)
    // This is a placeholder; actual system vibration picker might not be available or work this way.
    // A custom selection or pattern creation UI might be needed.
    private val vibrationPatternPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle result from vibration picker (if a system picker exists and returns data)
            // This part is highly dependent on the system's implementation.
            // For now, we'll just log a message.
            Timber.d("Vibration pattern picker returned with result code: ${result.resultCode}")
            // You would typically get the selected pattern data here and update the UI
        }
    }


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

            actvVibrationPattern.setOnClickListener {
                //openVibrationPatternPicker() // Use this if a system picker is available
                // Or implement custom vibration pattern selection/creation UI here
                Timber.d("Vibration Pattern field clicked. Implement vibration pattern selection.")
                // For now, let's just simulate setting a pattern or clear it
                if (selectedVibrationPattern == null) {
                    selectedVibrationPattern = longArrayOf(0, 100, 200, 300) // Example pattern
                    actvVibrationPattern.setText("Custom Pattern (Example)", false)
                } else {
                    selectedVibrationPattern = null
                    actvVibrationPattern.setText("", false)
                }
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

    // Placeholder for opening vibration pattern picker or custom UI
    private fun openVibrationPatternPicker() {
        // System vibration picker is not as standardized as sound picker.
        // You might need to implement your own UI for selecting/creating vibration patterns.
        Timber.d("Attempting to open vibration pattern picker (System picker may not be available).")
        // Example of launching an intent, but this might not work on all devices
        // val intent = Intent(Vibrator.ACTION_VIEW_VIBRATION_PATTERN) // This action might not exist
        // vibrationPatternPickerLauncher.launch(intent)
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


            // Load Vibration Pattern (Assuming it's stored as a String representation of LongArray)
            dataMap["vibrationPattern"]?.let { patternData ->
                // You'll need to parse the stored data back into a LongArray
                // This depends on how you choose to store the vibration pattern
                // For now, we'll just check if data exists and set a placeholder text
                binding.actvVibrationPattern.setText("Custom Pattern Loaded", false)
                // If you stored the pattern as a comma-separated string:
                try {
                    val patternString = patternData.toString().removePrefix("[").removeSuffix("]")
                    selectedVibrationPattern = patternString.split(",").map { it.trim().toLong() }.toLongArray()
                    binding.actvVibrationPattern.setText("Custom Pattern Loaded", false) // Update UI to reflect loaded pattern
                } catch (e: Exception) {
                    Timber.e("Error parsing vibration pattern: ${e.message}")
                    binding.actvVibrationPattern.setText("Error Loading Pattern", false) // Indicate error
                }

            } ?: run {
                // Set a default vibration pattern if none is saved
                // Or leave it empty and let the system default apply
                binding.actvVibrationPattern.setText("System Default", false)
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
        val vibrationPatternString = selectedVibrationPattern?.joinToString(",") // Convert LongArray to String
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
                if (vibrationPatternString != null) {
                    this["vibrationPattern"] = vibrationPatternString // Save as String
                }
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
