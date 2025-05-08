package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionCategory
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.databinding.DialogActionSelectionBinding
import com.example.rutinas.ui.main.adapter.ActionCategoriesAdapter
import timber.log.Timber

class ActionCategoryDialog : DialogFragment() {

    // Modificación: La interfaz ahora pasa solo ActionType
    interface ActionSelectionListener {
        fun onActionSelected(actionType: ActionType) // <-- Cambiado a ActionType
    }

    private var _binding: DialogActionSelectionBinding? = null
    private val binding get() = _binding!!
    private var actionSelectionListener: ActionSelectionListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("ActionCategoryDialog: onCreateDialog() called")
        _binding = DialogActionSelectionBinding.inflate(LayoutInflater.from(context))

        val categories = getActionCategories()
        val adapter = ActionCategoriesAdapter(requireContext(), categories.map { it.name }, getActionsMap(categories))
        binding.expandableListView.setAdapter(adapter)
        binding.expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val category = categories[groupPosition]
            val actionType = category.actions[childPosition]
            onActionTypeSelected(actionType) // Pasa solo el ActionType
            true
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    // Modificación: Pasar solo ActionType a través del listener
    private fun onActionTypeSelected(actionType: ActionType) {
        Timber.d("ActionCategoryDialog: onActionTypeSelected() called with actionType: $actionType")
        // Ya no creamos el objeto Action aquí.
        // Solo notificamos al listener qué tipo de acción fue seleccionada.
        actionSelectionListener?.onActionSelected(actionType) // <-- Pasa solo actionType
        dismiss()
    }

    private fun getActionCategories(): List<ActionCategory> {
        return listOf(
            ActionCategory(
                "Utilidades", listOf(ActionType.ALARM, ActionType.TIME, ActionType.PAUSE)
            ),
            ActionCategory(
                "Dispositivo", listOf(ActionType.VOLUME, ActionType.SOUND_MODE, ActionType.BRIGHTNESS)
            ),
            ActionCategory(
                "Comunicación", listOf(ActionType.ANNOUNCEMENT, ActionType.READ_NOTIFICATIONS)
            )
        )
    }

    private fun getActionsMap(categories: List<ActionCategory>): HashMap<String, List<String>> {
        val actionsMap = hashMapOf<String, List<String>>()
        categories.forEach { category ->
            actionsMap[category.name] = category.actions.map { it.name }
        }
        return actionsMap
    }

    fun setActionSelectionListener(listener: ActionSelectionListener) {
        this.actionSelectionListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ActionCategoryDialog {
            return ActionCategoryDialog()
        }
    }
}