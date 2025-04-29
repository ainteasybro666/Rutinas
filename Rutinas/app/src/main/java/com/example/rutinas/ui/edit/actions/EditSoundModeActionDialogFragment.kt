package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditSoundModeActionBinding
import timber.log.Timber

class EditSoundModeActionDialogFragment : BaseEditActionDialogFragment() {
    private var _binding: FragmentEditSoundModeActionBinding? = null
    private val binding get() = _binding!!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditSoundModeActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditSoundModeActionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ -> saveAction() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
        loadActionData()
        return dialog
    }
    private fun loadActionData() {
        Timber.d("EditSoundModeActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    binding.etValue.setText(data["value"]?.toString() ?: "NORMAL")
                }
            }
        } catch (e: Exception) {
            Timber.e("EditSoundModeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }
    private fun saveAction() {
        Timber.d("EditSoundModeActionDialogFragment: saveAction() llamado")
        try {
            val value = binding.etValue.text.toString()
            val updatedAction = action.copy(
                data = DataWrapper(
                    mapOf(
                        "value" to value
                    )
                )
            )
            actionUpdateListener?.invoke(updatedAction)
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
        const val ARG_ACTION = "arg_action"
        fun newInstance(action: Action): EditSoundModeActionDialogFragment {
            return EditSoundModeActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}
