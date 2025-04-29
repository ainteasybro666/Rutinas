package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAnnouncementActionBinding
import timber.log.Timber

class EditAnnouncementActionDialogFragment : BaseEditActionDialogFragment() {
    private var _binding: FragmentEditAnnouncementActionBinding? = null
    private val binding get() = _binding!!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditAnnouncementActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditAnnouncementActionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ -> saveAction() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
        loadActionData()
        return dialog
    }
    private fun loadActionData() {
        Timber.d("EditAnnouncementActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    binding.etMessage.setText(data["message"] as? String ?: "")
                }
            }
        } catch (e: Exception) {
            Timber.e("EditAnnouncementActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }
    private fun saveAction() {
        Timber.d("EditAnnouncementActionDialogFragment: saveAction() llamado")
        try {
            val message = binding.etMessage.text.toString()
            val updatedAction = action.copy(
                data = DataWrapper(
                    mapOf(
                        "message" to message
                    )
                )
            )
            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditAnnouncementActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditAnnouncementActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }
    override fun onDestroyView() {
        Timber.d("EditAnnouncementActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }
    companion object {
        const val ARG_ACTION = "arg_action"
        fun newInstance(action: Action): EditAnnouncementActionDialogFragment {
            return EditAnnouncementActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}
