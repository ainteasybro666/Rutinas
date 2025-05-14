package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.databinding.FragmentEditTimeActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import com.example.rutinas.ui.edit.actions.BaseEditActionDialogFragment.Companion.newBundle
import timber.log.Timber

// This dialog is no longer needed as Time action is added directly
// Keeping for compatibility if it's used for editing existing ones.
class EditTimeActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment<FragmentEditTimeActionBinding>(listener) { // Heredar con el tipo de binding

    companion object {
        fun newInstance(
            action: Action,
            listener: ActionDialogListener
        ): EditTimeActionDialogFragment {
            // This fragment is no longer used for new Time actions, but keeping for compatibility
            // if it's used for editing existing ones.
            return EditTimeActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditTimeActionBinding { // Cambiar tipo de retorno
        // You might need to adjust this if the layout file is also removed
        Timber.d("EditTimeActionDialogFragment: Inflating binding")
        return FragmentEditTimeActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditTimeActionDialogFragment: onViewCreated() called. This dialog should ideally not be shown for new Time actions.")
        // No need to load data or set up UI elements related to time selection
        loadActionData(actionToEdit) // Llamar a loadActionData, aunque no haga nada
    }

    // Implementar loadActionData según el método abstracto de la base (no hace nada, ya que no hay datos)
    override fun loadActionData(action: Action?) {
        Timber.d("EditTimeActionDialogFragment: loadActionData() called (no data to load).")
        // No hay datos específicos del usuario para la acción de Tiempo
    }

    // Implementar saveActionData según el método abstracto de la base (no hace nada, ya que no hay datos)
    override fun saveActionData(): Action { // Implementar método abstracto
        Timber.d("EditTimeActionDialogFragment: saveActionData() called (no data to save).")
        // Since Time action is added directly, this method is primarily for editing existing Time actions.
        // If you allow editing a Time action, you might need to handle saving changes here,
        // although the Time action typically doesn't have editable data.
        // For now, return the current action object.
        return actionToEdit // Retornar la acción sin modificarla (o una copia si quieres ser explícito)
    }

    // Implementar onSaveAction
    override fun onSaveAction() {
        Timber.d("EditTimeActionDialogFragment: onSaveAction() called (no data to save).")
        try {
            val updatedAction = saveActionData()
            notifyActionUpdated(updatedAction)
            Timber.d("EditTimeActionDialogFragment: Action updated and notified (no data to save).")
            dismiss()
        } catch (e: Exception) {
            Timber.e("EditTimeActionDialogFragment: Error saving action - ${e.message}")
            showErrorDialog("Error al guardar la acción: ${e.message}")
        }
    }
}
