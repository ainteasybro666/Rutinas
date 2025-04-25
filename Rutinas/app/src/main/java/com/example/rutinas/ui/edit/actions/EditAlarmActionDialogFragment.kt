package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAlarmActionBinding
import com.example.rutinas.ui.edit.RoutineEditFragment
import timber.log.Timber

class EditAlarmActionDialogFragment(private val listener: RoutineEditFragment.ActionDialogListener) : BaseEditActionDialogFragment(listener), ActionEditorDialog {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditAlarmActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditAlarmActionBinding.inflate(layoutInflater)

        setupUI()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ -> saveAction() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()

        loadActionData()
        return dialog
    }

    private fun setupUI() {
        Timber.d("EditAlarmActionDialogFragment: setupUI() llamado")
        with(binding) {
            // Configurar el adaptador del Spinner
            val durations = listOf("10 segundos", "30 segundos", "1 minuto", "2 minutos", "5 minutos")
            spinnerDuration.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                durations
            )


            // Configurar el switch de repetir
            switchRepeat.setOnCheckedChangeListener { _, isChecked ->
                Timber.d("EditAlarmActionDialogFragment: Switch Repetir cambiado a $isChecked")
                repeatOptionsGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadActionData() {
        Timber.d("EditAlarmActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { dataWrapper ->
                dataWrapper?.data?.let { data ->
                    val duration = (data["duration"] as? Int) ?: 30 // Valor predeterminado si es nulo
                    val repeatEnabled = (data["repeatEnabled"] as? Boolean) ?: false // Valor predeterminado si es nulo

                    Timber.d("EditAlarmActionDialogFragment: Datos cargados - duración: $duration, repetir: $repeatEnabled")

                    val durationValues = listOf(10, 30, 60, 120, 300) // Lista de valores de duración
                    val durationIndex = durationValues.indexOf(duration).takeIf { it != -1 } ?: 1 // Obtener el índice del valor o usar 1 si no se encuentra

                    binding.spinnerDuration.setSelection(durationIndex) // Establecer la selección del Spinner

                    binding.switchRepeat.isChecked = repeatEnabled
                    binding.repeatOptionsGroup.visibility = if (repeatEnabled) View.VISIBLE else View.GONE
                }
            }
        } catch (e: Exception) {
            Timber.e("EditAlarmActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    private fun saveAction() {
        Timber.d("EditAlarmActionDialogFragment: saveAction() llamado")
        try {
            val durationValues = listOf(10, 30, 60, 120, 300)
            val durationIndex = binding.spinnerDuration.selectedItemPosition
            val duration = durationValues[durationIndex]
            val repeatEnabled = binding.switchRepeat.isChecked

            Timber.d("EditAlarmActionDialogFragment: Guardando - duración: $duration, repetir: $repeatEnabled")

            val updatedAction = action.copy(
                data = DataWrapper(
                    mapOf(
                        "duration" to duration,
                        "repeatEnabled" to repeatEnabled
                    )
                )
            )
            actionUpdateListener?.invoke(updatedAction)
            listener.onActionUpdated(updatedAction)
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

    companion object {
        const val ARG_ACTION = "arg_action"

        fun newInstance(
            action: Action, listener: RoutineEditFragment.ActionDialogListener
        ): EditAlarmActionDialogFragment {
            return EditAlarmActionDialogFragment(listener).apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}
