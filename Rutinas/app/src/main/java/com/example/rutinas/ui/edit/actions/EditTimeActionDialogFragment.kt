package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditTimeActionBinding // Keep import for binding class if it exists
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

// This dialog is no longer needed as Time action is added directly
class EditTimeActionDialogFragment(listener: ActionDialogListener) :
    BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditTimeActionBinding? = null
    private val binding get() = _binding!!

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
    ): ViewBinding {
        // You might need to adjust this if the layout file is also removed
        _binding = FragmentEditTimeActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditTimeActionDialogFragment: onViewCreated() called. This dialog should ideally not be shown for new Time actions.")
        // No need to load data or set up UI elements related to time selection
        // loadActionData() // Remove this call
    }

    override fun saveAction() {
        Timber.d("EditTimeActionDialogFragment: saveAction() called. This should ideally not be called for new Time actions.")
        // Since Time action is added directly, this method is primarily for editing existing Time actions.
        // If you allow editing a Time action, you might need to handle saving changes here,
        // although the Time action typically doesn't have editable data.
        // For now, I'll keep a placeholder but the logic will depend on whether you allow editing.
        try {
            // Assuming no editable data for Time action, just notify the listener
            // with the current action object.
            notifyActionUpdated(action)
            Timber.d("EditTimeActionDialogFragment: Action updated and notified (no data to save).")
        } catch (e: Exception) {
            Timber.e("EditTimeActionDialogFragment: Error saving action - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditTimeActionDialogFragment: onDestroyView() called")
        super.onDestroyView()
        _binding = null
    }
}
