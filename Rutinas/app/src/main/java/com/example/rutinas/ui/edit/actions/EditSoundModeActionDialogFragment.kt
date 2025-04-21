package com.example.rutinas.ui.edit.actions

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditSoundModeActionBinding
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
class EditSoundModeActionDialogFragment : DialogFragment(), Parcelable, ActionEditorDialog {
    private var _binding: FragmentEditSoundModeActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action
    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditSoundModeActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditSoundModeActionBinding.inflate(layoutInflater)

        action = requireArguments().getParcelable(ARG_ACTION)
            ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

        Timber.d("EditSoundModeActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

        loadActionData()

        val dialog =  super.onCreateDialog(savedInstanceState) as AlertDialog
        return AlertDialog.Builder(requireContext())
            .setTitle("Modo de Sonido")
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ ->
                Timber.d("EditSoundModeActionDialogFragment: Botón Guardar pulsado")
                saveAction()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Timber.d("EditSoundModeActionDialogFragment: Botón Cancelar pulsado")
            }
            .create()
    }

    private fun loadActionData() {
        Timber.d("EditSoundModeActionDialogFragment: loadActionData() llamado")
        try {
            action.data["mode"]?.let { mode ->
                Timber.d("EditSoundModeActionDialogFragment: Modo cargado: $mode")
                when (mode) {
                    "silent" -> binding.radioSilent.isChecked = true
                    "vibrate" -> binding.radioVibration.isChecked = true
                    "normal" -> binding.radioNormal.isChecked = true
                }
            }
        } catch (e: Exception) {
            Timber.e("EditSoundModeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    private fun saveAction() {
        Timber.d("EditSoundModeActionDialogFragment: saveAction() llamado")
        try {
            val mode = when {
                binding.radioSilent.isChecked -> "silent"
                binding.radioVibration.isChecked -> "vibrate"
                else -> "normal"
            }

            Timber.d("EditSoundModeActionDialogFragment: Guardando modo: $mode")

            val updatedAction = action.copy(
                data = mapOf("mode" to mode)
            )

            onActionUpdatedListener?.invoke(updatedAction)
            Timber.d("EditSoundModeActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditSoundModeActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditSoundModeActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action) = EditSoundModeActionDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_ACTION, action)
            }
        }
    }
}