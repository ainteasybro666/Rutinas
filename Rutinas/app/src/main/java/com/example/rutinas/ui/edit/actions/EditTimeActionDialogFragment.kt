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
class EditTimeActionDialogFragment : DialogFragment(), Parcelable {
    private var _binding: FragmentEditTimeActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action
    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditTimeActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditTimeActionBinding.inflate(layoutInflater)

        try {
            action = requireArguments().getSerializable(ARG_ACTION) as? Action
                ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

            Timber.d("EditTimeActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

            setupUI()
            loadActionData()

            return AlertDialog.Builder(requireContext())
                .setTitle("Configurar Hora")
                .setView(binding.root)
                .setPositiveButton("Guardar") { _, _ ->
                    Timber.d("EditTimeActionDialogFragment: Botón Guardar pulsado")
                    saveAction()
                }
                .setNegativeButton("Cancelar") { _, _ ->
                    Timber.d("EditTimeActionDialogFragment: Botón Cancelar pulsado")
                }
                .create()
        } catch (e: Exception) {
            Timber.e("EditTimeActionDialogFragment: Error en onCreateDialog - ${e.message}")
            e.printStackTrace()

            // Crear un diálogo de error en caso de fallo
            return AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage("No se pudo cargar la acción: ${e.message}")
                .setPositiveButton("Aceptar") { _, _ ->
                    dismiss()
                }
                .create()
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

            onActionUpdatedListener?.invoke(updatedAction)
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