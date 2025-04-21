package com.example.rutinas.ui.edit.actions

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditTimeActionBinding
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
class EditTimeActionDialogFragment
    : BaseEditActionDialogFragment(),      // hereda `action` y `listener`
    ActionEditorDialog,
    Parcelable {

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        actionUpdateListener = listener
    }
    private var _binding: FragmentEditTimeActionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditTimeActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditTimeActionBinding.inflate(layoutInflater)

        try {
            action = requireArguments().getParcelable<Action>(ARG_ACTION)
                ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

            Timber.d("EditTimeActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

            setupUI()
            loadActionData()

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Configurar Hora").setView(binding.root)
                .setPositiveButton("Guardar") { _, _ -> saveAction() }
                .setNegativeButton("Cancelar", null)
                .create()
            return dialog
        } catch (e: Exception){
            Timber.e("EditTimeActionDialogFragment: Error creating dialog - ${e.message}")

            val dialog = AlertDialog.Builder(requireContext()).setTitle("Error").setMessage("Failed to load action")
                .setPositiveButton("OK", null)
                .create()
            return dialog
        }
    }

    private fun setupUI() {
        Timber.d("EditTimeActionDialogFragment: setupUI() llamado")
        with(binding) {
            radio12h.setOnCheckedChangeListener { _, isChecked ->
                Timber.d("EditTimeActionDialogFragment: Radio 12h cambiado a $isChecked")
                // Actualizar vista previa si es necesario
            }
            radio24h.setOnCheckedChangeListener { _, isChecked ->
                Timber.d("EditTimeActionDialogFragment: Radio 24h cambiado a $isChecked")
                // Actualizar vista previa si es necesario
            }
        }
    }

    private fun loadActionData() {
        Timber.d("EditTimeActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { data ->
                val timeFormat = data["timeFormat"] as? String ?: "24h"
                Timber.d("EditTimeActionDialogFragment: Formato de hora cargado: $timeFormat")

                binding.radio12h.isChecked = timeFormat == "12h"
                binding.radio24h.isChecked = timeFormat == "24h"
            }
        } catch (e: Exception) {
            Timber.e("EditTimeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    private fun saveAction() {
        Timber.d("EditTimeActionDialogFragment: saveAction() llamado")
        try {
            val timeFormat = if (binding.radio12h.isChecked) "12h" else "24h"
            Timber.d("EditTimeActionDialogFragment: Guardando formato de hora: $timeFormat")

            val updatedAction = action.copy(
                data = mapOf(
                    "timeFormat" to timeFormat
                )
            )

            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditTimeActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditTimeActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditTimeActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action) = EditTimeActionDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_ACTION, action)
            }
        }
    }
}