package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.app.AlertDialog
import android.os.Bundle
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditSoundModeActionBinding
import com.example.rutinas.ui.edit.RoutineEditFragment
import timber.log.Timber

class EditSoundModeActionDialogFragment(listener: RoutineEditFragment.ActionDialogListener) : BaseEditActionDialogFragment(listener), ActionEditorDialog {
    private var _binding: FragmentEditSoundModeActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditSoundModeActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditSoundModeActionBinding.inflate(layoutInflater)

        Timber.d("EditSoundModeActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

        loadActionData()

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
            action.data?.let { dataWrapper ->
                val mode = dataWrapper.data["mode"]
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
                data = DataWrapper(mapOf(
                    "mode" to mode
                ))
            )

            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditSoundModeActionDialogFragment: Acción actualizada y notificada")
            listener.onActionUpdated(updatedAction)
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

        fun newInstance(
            action: Action,
            listener: RoutineEditFragment.ActionDialogListener
        ): EditSoundModeActionDialogFragment {
            return EditSoundModeActionDialogFragment(listener).apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }    
    }
}