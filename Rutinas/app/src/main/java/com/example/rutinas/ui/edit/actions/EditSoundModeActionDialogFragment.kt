package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditSoundModeActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

class EditSoundModeActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditSoundModeActionBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditSoundModeActionDialogFragment {
            return EditSoundModeActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentEditSoundModeActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditSoundModeActionDialogFragment: onViewCreated() llamado")
        loadActionData()
    }

    private fun loadActionData() {
        Timber.d("EditSoundModeActionDialogFragment: loadActionData() llamado")
        try {
            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    val value = data["value"] as? String ?: "NORMAL"
                    when (value) {
                        "NORMAL" -> binding.rbNormal.isChecked = true
                        "SILENT" -> binding.rbSilent.isChecked = true
                        "VIBRATE" -> binding.rbVibrate.isChecked = true
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("EditSoundModeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveAction() {
        Timber.d("EditSoundModeActionDialogFragment: saveAction() llamado")
        try {
            val value = when (binding.radioGroupSoundMode.checkedRadioButtonId) {
                binding.rbNormal.id -> "NORMAL"
                binding.rbSilent.id -> "SILENT"
                binding.rbVibrate.id -> "VIBRATE"
                else -> "NORMAL"
            }

            val updatedAction = action.copy(
                actionType = ActionType.SOUND_MODE,
                data = DataWrapper(
                    mapOf(
                        "value" to value
                    )
                )
            )
            notifyActionUpdated(updatedAction)
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
}
