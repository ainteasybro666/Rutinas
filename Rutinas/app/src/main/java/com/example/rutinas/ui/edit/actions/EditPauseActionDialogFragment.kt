package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditPauseActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

class EditPauseActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditPauseActionBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditPauseActionDialogFragment {
            return EditPauseActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentEditPauseActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditPauseActionDialogFragment: onViewCreated() llamado")
        setupNumberPickers()
        loadActionData()
    }

    private fun setupNumberPickers() {
        binding.npHours.apply {
            minValue = 0
            maxValue = 23
        }
        binding.npMinutes.apply {
            minValue = 0
            maxValue = 59
        }
        binding.npSeconds.apply {
            minValue = 0
            maxValue = 59
        }
    }

    private fun loadActionData() {
        Timber.d("EditPauseActionDialogFragment: loadActionData() llamado")
        try {
            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    val totalSeconds = data["value"] as? Long ?: 0
                    val hours = (totalSeconds / 3600).toInt()
                    val minutes = ((totalSeconds % 3600) / 60).toInt()
                    val seconds = (totalSeconds % 60).toInt()

                    binding.npHours.value = hours
                    binding.npMinutes.value = minutes
                    binding.npSeconds.value = seconds
                }
            }
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveAction() {
        Timber.d("EditPauseActionDialogFragment: saveAction() llamado")
        try {
            val hours = binding.npHours.value
            val minutes = binding.npMinutes.value
            val seconds = binding.npSeconds.value
            val totalSeconds = hours * 3600 + minutes * 60 + seconds

            val updatedAction = action.copy(
                actionType = ActionType.PAUSE,
                data = DataWrapper(
                    mapOf(
                        "value" to totalSeconds
                    )
                )
            )
            notifyActionUpdated(updatedAction)
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
}
