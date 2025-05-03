package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.ui.edit.ActionDialogListener
import com.example.rutinas.ui.edit.RoutineEditFragment
import timber.log.Timber

abstract class BaseEditActionDialogFragment(protected val listener: ActionDialogListener) : DialogFragment() {

    // To avoid showing the error dialog multiple times.
    private var errorDialogShown = false

    companion object {
        const val ARG_ACTION = "arg_action"
        fun newBundle(action: Action) = bundleOf(ARG_ACTION to action)
    }

    protected lateinit var action: Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getParcelable(ARG_ACTION, Action::class.java)
                    ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            } else {
                requireArguments().getParcelable(ARG_ACTION)
                    ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            }
            Timber.d("${this.javaClass.simpleName}: Acción recibida - tipo: ${action.actionType}, datos: ${action.data?.data}")
        } catch (e: Exception) {
            Timber.e("${this.javaClass.simpleName}: Error al obtener la acción - ${e.message}")
            e.printStackTrace()
            requireActivity().supportFragmentManager.popBackStack()
            // Show error dialog only once.
            if (!errorDialogShown) {
                errorDialogShown = true
                showErrorDialog(e.message ?: "Error desconocido")
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return try {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setView(inflateBinding(layoutInflater, null).root)
            dialogBuilder.setPositiveButton("Guardar") { _, _ -> saveAction() }
            dialogBuilder.setNegativeButton("Cancelar") { _, _ -> dismiss() }
            val dialog = dialogBuilder.create()
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog
        } catch (e: Exception) {
            Timber.e("BaseEditActionDialogFragment: Error al crear el diálogo - ${e.message}")
            e.printStackTrace()
            AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage("No se pudo cargar la acción: ${e.message}")
                .setPositiveButton("Aceptar") { _, _ ->
                    dismiss()
                }.create()
        }
    }

    protected fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding
    protected abstract fun saveAction()
    protected fun notifyActionUpdated(updatedAction: Action){
        listener.onActionUpdated(updatedAction)
    }
}
