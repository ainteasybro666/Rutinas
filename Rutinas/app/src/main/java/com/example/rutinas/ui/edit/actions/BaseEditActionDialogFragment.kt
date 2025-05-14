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
import androidx.viewbinding.ViewBinding // Importar ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

// Eliminar @AndroidEntryPoint de aquí
// @AndroidEntryPoint
abstract class BaseEditActionDialogFragment<T : ViewBinding>(protected val listener: ActionDialogListener) : DialogFragment() { // Volver a usar genérico T

    // To avoid showing the error dialog multiple times.
    private var errorDialogShown = false

    // Declarar _binding como protected y inicialízalo en la subclase
    protected var _binding: T? = null
    protected val binding get() = _binding!!

    protected lateinit var actionToEdit: Action // Cambiar nombre para mayor claridad

    companion object {
        const val ARG_ACTION = "arg_action"
        fun newBundle(action: Action) = bundleOf(ARG_ACTION to action)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            actionToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getParcelable(ARG_ACTION, Action::class.java)
                    ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            } else {
                @Suppress("DEPRECATION")
                requireArguments().getParcelable(ARG_ACTION)
                    ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            }
            Timber.d("${this.javaClass.simpleName}: Acción recibida - tipo: ${actionToEdit.actionType}, datos: ${actionToEdit.data?.data}")
        } catch (e: Exception) {
            Timber.e("${this.javaClass.simpleName}: Error al obtener la acción - ${e.message}")
            e.printStackTrace()
            if (!errorDialogShown) {
                errorDialogShown = true
                showErrorDialog(e.message ?: "Error desconocido")
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return try {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            // Inflar el binding en la subclase y pasarlo a setView
            _binding = inflateBinding(layoutInflater, null)
            dialogBuilder.setView(binding.root)


            // Use safe call and Elvis operator to provide a default empty View if binding is null
            _binding?.let {
                    dialogBuilder.setView(it.root)
                }

            // La configuración de botones la manejarán las subclases
            // dialogBuilder.setPositiveButton("Guardar") { _, _ -> onSaveAction() }
            // dialogBuilder.setNegativeButton("Cancelar") { _, _ -> dismiss() }

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
            showErrorDialog("Error al cargar la configuración de la acción: ${e.message}")
            AlertDialog.Builder(requireContext()).create()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpiar el binding
    }

    protected fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Aceptar") { _, _ ->
                dismiss()
            }
            .show()
    }

    // Cambiar el método abstracto para que devuelva el genérico T
    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T

    protected abstract fun onSaveAction()
    protected abstract fun loadActionData(action: Action?)
    protected abstract fun saveActionData(): Action

    protected fun notifyActionUpdated(updatedAction: Action){
        listener.onActionUpdated(updatedAction)
    }
}