package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditTimeActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

class EditTimeActionDialogFragment(listener: ActionDialogListener) :
    BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditTimeActionBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(
            action: Action,
            listener: ActionDialogListener
        ): EditTimeActionDialogFragment {
            return EditTimeActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding {
        _binding = FragmentEditTimeActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditTimeActionDialogFragment: onViewCreated() llamado")
        loadActionData()
    }

    private fun loadActionData() {
        Timber.d("EditTimeActionDialogFragment: loadActionData() llamado")
        try {
            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    val value = data["value"] as? Long ?: 0
                    val hours = (value / 3600).toInt()
                    val minutes = ((value % 3600) / 60).toInt()
                    binding.tpTime.hour = hours
                    binding.tpTime.minute = minutes
                }
            }
        } catch (e: Exception) {
            Timber.e("EditTimeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveAction() {
        Timber.d("EditTimeActionDialogFragment: saveAction() llamado")
        try {
            val hours = binding.tpTime.hour
            val minutes = binding.tpTime.minute
            val totalSeconds = hours * 3600 + minutes * 60

            val updatedAction = action.copy(
                actionType = ActionType.TIME,
                data = DataWrapper(
                    mapOf(
                        "value" to totalSeconds
                    )
                )
            )
            notifyActionUpdated(updatedAction)
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
}
