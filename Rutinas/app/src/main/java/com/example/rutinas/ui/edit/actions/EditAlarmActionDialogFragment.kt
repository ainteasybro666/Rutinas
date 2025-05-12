package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.viewbinding.ViewBinding
import com.example.rutinas.R // Import your R file for accessing resources
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAlarmActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

class EditAlarmActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditAlarmActionBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val ARG_ACTION = "arg_action"

        fun newInstance(
            action: Action,
            listener: ActionDialogListener
        ): EditAlarmActionDialogFragment {
            return EditAlarmActionDialogFragment(listener).apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action) // Use putParcelable if Action is Parcelable
                    // putSerializable(ARG_ACTION, action) // Use putSerializable if Action is Serializable
                }
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentEditAlarmActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditAlarmActionDialogFragment: onViewCreated() llamado")

        // Setup Spinners
        setupSpinners()

        loadActionData()
    }

    private fun setupSpinners() {
        // Setup Duration Spinner
        val durationPresets = resources.getStringArray(R.array.duration_presets)
        val durationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, durationPresets)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.etDuration.adapter = durationAdapter // Use binding.etDuration

        // Setup Repetitions Spinner
        val repetitionCounts = resources.getStringArray(R.array.repetition_counts)
        val repetitionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, repetitionCounts)
        repetitionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.etRepetitions.adapter = repetitionAdapter // Use binding.etRepetitions
    }


    private fun loadActionData() {
        Timber.d("EditAlarmActionDialogFragment: loadActionData() llamado")
        try {
            val action = arguments?.getParcelable(ARG_ACTION) as? Action // Use getParcelable if Action is Parcelable
            // val action = arguments?.getSerializable(ARG_ACTION) as? Action // Use getSerializable if Action is Serializable

            if (action == null) {
                Timber.e("EditAlarmActionDialogFragment: Error al obtener la acción de los argumentos.")
                dismiss()
                return
            }
            this.action = action

            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    // Load existing data for TimePicker and Label
                    val hour = (data["hour"] as? Int) ?: 0
                    val minute = (data["minute"] as? Int) ?: 0
                    val label = data["label"] as? String ?: ""

                    binding.timePicker.hour = hour
                    binding.timePicker.minute = minute
                    binding.etLabel.setText(label)

                    // Load data for Spinners and set selection
                    val savedDurationPreset = data["durationPreset"] as? String
                    val savedRepetitionsCount = data["repetitionsCount"] as? String

                    // Find and set selection for Duration Spinner
                    val durationPresets = resources.getStringArray(R.array.duration_presets)
                    val durationIndex = durationPresets.indexOf(savedDurationPreset)
                    if (durationIndex != -1) {
                        binding.etDuration.setSelection(durationIndex) // Use binding.etDuration
                    } else {
                        // Default: Select "normal" if not found (index 2)
                        val normalIndex = durationPresets.indexOf("normal")
                        if (normalIndex != -1) binding.etDuration.setSelection(normalIndex) // Use binding.etDuration
                        else binding.etDuration.setSelection(0) // Fallback to first item
                    }


                    // Find and set selection for Repetitions Spinner
                    val repetitionCounts = resources.getStringArray(R.array.repetition_counts)
                    val repetitionsIndex = repetitionCounts.indexOf(savedRepetitionsCount)
                    if (repetitionsIndex != -1) {
                        binding.etRepetitions.setSelection(repetitionsIndex) // Use binding.etRepetitions
                    } else {
                        // Default: Select "1" repetition if not found (index 0)
                        val defaultRepetitionsIndex = repetitionCounts.indexOf("1")
                        if (defaultRepetitionsIndex != -1) binding.etRepetitions.setSelection(defaultRepetitionsIndex) // Use binding.etRepetitions
                        else binding.etRepetitions.setSelection(0) // Fallback to first item
                    }

                    Timber.d("EditAlarmActionDialogFragment: Datos cargados - hora: $hour:$minute, etiqueta: $label, duracionPreset: $savedDurationPreset, repeticionesCount: $savedRepetitionsCount")
                }
            }
        } catch (e: Exception) {
            Timber.e("EditAlarmActionDialogFragment: Error al cargar datos - ${e.message}")
            dismiss()
        }
    }

    override fun saveAction() {
        Timber.d("EditAlarmActionDialogFragment: saveAction() llamado")
        try {
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val label = binding.etLabel.text.toString()

            // Get selected values from Spinners
            val selectedDurationPreset = binding.etDuration.selectedItem.toString() // Use binding.etDuration
            val selectedRepetitionsCount = binding.etRepetitions.selectedItem.toString() // Use binding.etRepetitions

            Timber.d("EditAlarmActionDialogFragment: Guardando - hora: $hour:$minute, etiqueta: $label, duracionPreset: $selectedDurationPreset, repeticionesCount: $selectedRepetitionsCount")

            val updatedAction = action.copy(
                actionType = ActionType.ALARM,
                data = DataWrapper(
                    mapOf(
                        "hour" to hour,
                        "minute" to minute,
                        "label" to label,
                        "durationPreset" to selectedDurationPreset, // Save the selected preset string
                        "repetitionsCount" to selectedRepetitionsCount // Save the selected count string
                    )
                )
            )
            notifyActionUpdated(updatedAction)
            Timber.d("EditAlarmActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditAlarmActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditAlarmActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }
}
