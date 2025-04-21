package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditAlarmActionBinding
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
class EditAlarmActionDialogFragment : BaseEditActionDialogFragment(), ActionEditorDialog, Parcelable {
    private var _binding: FragmentEditAlarmActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action
    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditAlarmActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditAlarmActionBinding.inflate(layoutInflater)

        setupUI()
        loadActionData()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.setOnShowListener {
            Timber.d("EditAlarmActionDialogFragment: Diálogo mostrado")
        }

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

            // Configurar el botón "Guardar"
            btnSave.setOnClickListener {
                Timber.d("EditAlarmActionDialogFragment: Botón Guardar pulsado")
                saveAction()
                dismiss()
            }

            // Configurar la flecha de retroceso
            btnBack.setOnClickListener {
                Timber.d("EditAlarmActionDialogFragment: Botón Volver pulsado")
                dismiss()
            }

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
            action.data.let { data ->
                val duration = (data["duration"] as? Int) ?: 30
                val repeatEnabled = (data["repeatEnabled"] as? Boolean) ?: false

                Timber.d("EditAlarmActionDialogFragment: Datos cargados - duración: $duration, repetir: $repeatEnabled")

                // Mapear el valor de duración al índice del Spinner
                val durationValues = listOf(10, 30, 60, 120, 300)
                val durationIndex = durationValues.indexOf(duration).takeIf { it != -1 } ?: 1
                binding.spinnerDuration.setSelection(durationIndex)
                binding.switchRepeat.isChecked = repeatEnabled
                binding.repeatOptionsGroup.visibility = if (repeatEnabled) View.VISIBLE else View.GONE
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
                data = mapOf(
                    "duration" to duration,
                    "repeatEnabled" to repeatEnabled
                )
            )
            onActionUpdatedListener?.invoke(updatedAction)
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
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action): EditAlarmActionDialogFragment {
            return EditAlarmActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}