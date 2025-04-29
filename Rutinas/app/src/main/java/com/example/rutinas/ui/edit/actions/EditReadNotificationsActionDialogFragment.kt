package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditReadNotificationsActionBinding
import timber.log.Timber

class EditReadNotificationsActionDialogFragment : BaseEditActionDialogFragment() {
    private var _binding: FragmentEditReadNotificationsActionBinding? = null
    private val binding get() = _binding!!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditReadNotificationsActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditReadNotificationsActionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ -> saveAction() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
        loadActionData()
        return dialog
    }
    private fun loadActionData() {
        Timber.d("EditReadNotificationsActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { dataWrapper ->
                dataWrapper.data.let { data ->

                }
            }
        } catch (e: Exception) {
            Timber.e("EditReadNotificationsActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }
    private fun saveAction() {
        Timber.d("EditReadNotificationsActionDialogFragment: saveAction() llamado")
        try {
            val updatedAction = action.copy(
                data = DataWrapper(
                    mapOf(
                    )
                )
            )
            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditReadNotificationsActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditReadNotificationsActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }
    override fun onDestroyView() {
        Timber.d("EditReadNotificationsActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }
    companion object {
        const val ARG_ACTION = "arg_action"
        fun newInstance(action: Action): EditReadNotificationsActionDialogFragment {
            return EditReadNotificationsActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}
