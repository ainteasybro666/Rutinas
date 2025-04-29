package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditPauseActionBinding
import timber.log.Timber

class EditPauseActionDialogFragment : BaseEditActionDialogFragment() {
    private var _binding: FragmentEditPauseActionBinding? = null
    private val binding get() = _binding!!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditPauseActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditPauseActionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ -> saveAction() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
        loadActionData()
        return dialog
    }
    private fun loadActionData() {
        Timber.d("EditPauseActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    binding.etValue.setText(data["value"]?.toString() ?: "10")
                }
            }
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }
    private fun saveAction() {
        Timber.d("EditPauseActionDialogFragment: saveAction() llamado")
        try {
            val value = binding.etValue.text.toString().toLong()
            val updatedAction = action.copy(
                pauseDuration = value,
                data = DataWrapper(
                    mapOf(
                        "value" to value
                    )
                )
            )
            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditPauseActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }
    override fun onDestroyView() {
        Timber.d("EditPauseActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }
    companion object {
        const val ARG_ACTION = "arg_action"
        fun newInstance(action: Action): EditPauseActionDialogFragment {
            return EditPauseActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}
