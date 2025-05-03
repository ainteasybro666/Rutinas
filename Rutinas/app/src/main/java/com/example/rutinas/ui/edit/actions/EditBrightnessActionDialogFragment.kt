package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditBrightnessActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

class EditBrightnessActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditBrightnessActionBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditBrightnessActionDialogFragment {
            return EditBrightnessActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentEditBrightnessActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditBrightnessActionDialogFragment: onViewCreated() llamado")
        loadActionData()
    }

    private fun loadActionData() {
        Timber.d("EditBrightnessActionDialogFragment: loadActionData() llamado")
        try {
            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    val value = data["value"] as? Int ?: 100
                    val automatic = data["automatic"] as? Boolean ?: false
                    binding.sbBrightness.progress = value
                    binding.switchAutomatic.isChecked = automatic
                }
            }
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveAction() {
        Timber.d("EditBrightnessActionDialogFragment: saveAction() llamado")
        try {
            val value = binding.sbBrightness.progress
            val automatic = binding.switchAutomatic.isChecked
            val updatedAction = action.copy(
                actionType = ActionType.BRIGHTNESS,
                data = DataWrapper(
                    mapOf(
                        "value" to value,
                        "automatic" to automatic
                    )
                )
            )
            notifyActionUpdated(updatedAction)
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
}
