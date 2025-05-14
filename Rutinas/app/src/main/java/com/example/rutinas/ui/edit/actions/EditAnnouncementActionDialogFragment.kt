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
import com.example.rutinas.ui.edit.actions.BaseEditActionDialogFragment.Companion.newBundle
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class EditAnnouncementActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditAnnouncementActionBinding>(listener) { // Heredar con el tipo de binding y pasar listener

    // El binding se maneja en la clase base, solo lo declaramos aquí
    // private var _binding: FragmentEditAnnouncementActionBinding? = null
    // private val binding get() = _binding!! // Accedemos a través de 'binding' en la base

    companion object {
        fun newInstance(action: Action, listener: ActionDialogListener): EditAnnouncementActionDialogFragment {
            return EditAnnouncementActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    // Implementar inflateBinding para que devuelva el tipo de binding
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentEditAnnouncementActionBinding { // Cambiar tipo de retorno
        Timber.d("EditAnnouncementActionDialogFragment: Inflating binding")
        // Inflar y retornar el binding
        return FragmentEditAnnouncementActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditAnnouncementActionDialogFragment: onViewCreated() llamado")
        // Llamar a loadActionData de la base para cargar los datos
        loadActionData(actionToEdit)
    }

    // Implementar loadActionData según el método abstracto de la base
    override fun loadActionData(action: Action?) {
        Timber.d("EditAnnouncementActionDialogFragment: loadActionData() llamado")
        action?.let { actionToLoad -> // Usar un nombre claro para evitar confusión
            try {
                actionToLoad.data?.let { dataWrapper ->
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
        } ?: Timber.d("EditAnnouncementActionDialogFragment: No action data to load.")
    }

    // Implementar saveActionData según el método abstracto de la base
    override fun saveActionData(): Action {
        Timber.d("EditAnnouncementActionDialogFragment: saveActionData() llamado")
        val message = binding.etMessage.text.toString()
        val volume = binding.sbVolume.progress

        // Crear DataWrapper
        val data = DataWrapper(
            mutableMapOf(
                "message" to message as Any, // Cast a Any
                "volume" to volume as Any // Cast a Any
            )
        )

        // Retornar la Acción actualizada (usar actionToEdit para el UUID, etc.)
        return actionToEdit.copy(
            actionType = ActionType.ANNOUNCEMENT, // Asegurar el tipo correcto
            data = data
            // routineId, executionOrder, pauseDuration se copian de actionToEdit
        )
    }

    // Implementar onSaveAction para llamar a saveActionData y notificar
    override fun onSaveAction() {
        Timber.d("EditAnnouncementActionDialogFragment: onSaveAction() llamado")
        try {
            val updatedAction = saveActionData()
            notifyActionUpdated(updatedAction)
            Timber.d("EditAnnouncementActionDialogFragment: Acción actualizada y notificada")
            dismiss() // Cerrar el diálogo después de guardar
        } catch (e: Exception) {
            Timber.e("EditAnnouncementActionDialogFragment: Error al guardar acción - ${e.message}")
            showErrorDialog("Error al guardar la acción: ${e.message}")
        }
    }


    override fun onDestroyView() {
        Timber.d("EditAnnouncementActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        // El binding se limpia en la clase base
        // _binding = null
    }
}
