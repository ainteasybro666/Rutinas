package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAnnouncementActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import com.example.rutinas.ui.edit.RoutineEditFragment
import timber.log.Timber

class EditAnnouncementActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditAnnouncementActionBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditAnnouncementActionDialogFragment {
            return EditAnnouncementActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentEditAnnouncementActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditAnnouncementActionDialogFragment: onViewCreated() llamado")
        loadActionData()
    }

    private fun loadActionData() {
        Timber.d("EditAnnouncementActionDialogFragment: loadActionData() llamado")
        try {
            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    val message = data["message"] as? String ?: ""
                    val volume = data["volume"] as? Int ?: 50
                    binding.etMessage.setText(message)
                    binding.sbVolume.progress = volume
                }
            }
        } catch (e: Exception) {
            Timber.e("EditAnnouncementActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveAction() {
        Timber.d("EditAnnouncementActionDialogFragment: saveAction() llamado")
        try {
            val message = binding.etMessage.text.toString()
            val volume = binding.sbVolume.progress
            val updatedAction = action.copy(
                actionType = ActionType.ANNOUNCEMENT,
                data = DataWrapper(
                    mapOf(
                        "message" to message,
                        "volume" to volume
                    )
                )
            )
            notifyActionUpdated(updatedAction)
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
}