package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditBrightnessActionBinding
import timber.log.Timber

class EditBrightnessActionDialogFragment : BaseEditActionDialogFragment() {
    private var _binding: FragmentEditBrightnessActionBinding? = null
    private val binding get() = _binding!!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditBrightnessActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditBrightnessActionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ -> saveAction() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
        loadActionData()
        return dialog
    }
    private fun loadActionData() {
        Timber.d("EditBrightnessActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    binding.etValue.setText(data["value"]?.toString() ?: "100")
                }
            }
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }
    private fun saveAction() {
        Timber.d("EditBrightnessActionDialogFragment: saveAction() llamado")
        try {
            val value = binding.etValue.text.toString().toInt()
            val updatedAction = action.copy(
                data = DataWrapper(
                    mapOf(
                        "value" to value
                    )
                )
            )
            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditBrightnessActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }
    override fun onDestroyView() {
        Timber.d("EditBrightnessActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }
    companion object {
        const val ARG_ACTION = "arg_action"
        fun newInstance(action: Action): EditBrightnessActionDialogFragment {
            return EditBrightnessActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}
