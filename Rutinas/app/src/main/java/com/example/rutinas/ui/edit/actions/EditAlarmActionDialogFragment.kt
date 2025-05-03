package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAlarmActionBinding
import com.example.rutinas.ui.edit.RoutineEditFragment
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
        loadActionData()
    }

    private fun loadActionData() {
        Timber.d("EditAlarmActionDialogFragment: loadActionData() llamado")
        try {
            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    val hour = (data["hour"] as? Int) ?: 0
                    val minute = (data["minute"] as? Int) ?: 0
                    val label = data["label"] as? String ?: ""
                    val duration = data["duration"] as? String ?: ""
                    val repetitions = data["repetitions"] as? String ?: ""

                    Timber.d("EditAlarmActionDialogFragment: Datos cargados - hora: $hour:$minute, etiqueta: $label, duracion: $duration, repeticiones: $repetitions")

                    binding.timePicker.hour = hour
                    binding.timePicker.minute = minute
                    binding.etLabel.setText(label)
                    binding.etDuration.setText(duration)
                    binding.etRepetitions.setText(repetitions)
                }
            }
        } catch (e: Exception) {
            Timber.e("EditAlarmActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveAction() {
        Timber.d("EditAlarmActionDialogFragment: saveAction() llamado")
        try {
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val label = binding.etLabel.text.toString()
            val duration = binding.etDuration.text.toString()
            val repetitions = binding.etRepetitions.text.toString()

            Timber.d("EditAlarmActionDialogFragment: Guardando - hora: $hour:$minute, etiqueta: $label, duracion: $duration, repeticiones: $repetitions")

            val updatedAction = action.copy(
                actionType = ActionType.ALARM,
                data = DataWrapper(
                    mapOf(
                        "hour" to hour,
                        "minute" to minute,
                        "label" to label,
                        "duration" to duration,
                        "repetitions" to repetitions
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
