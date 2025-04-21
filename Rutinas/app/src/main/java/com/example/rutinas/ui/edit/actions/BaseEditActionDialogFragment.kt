package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import timber.log.Timber


abstract class BaseEditActionDialogFragment : DialogFragment(), ActionEditorDialog {

    // To avoid showing the error dialog multiple times.
    private var errorDialogShown = false
    protected var onActionUpdatedListener: ((Action) -> Unit)? = null

    companion object {
        const val ARG_ACTION = "arg_action"
    }

    protected lateinit var action: Action

    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            action = requireArguments().getParcelable<Action>(ARG_ACTION)
                ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            Timber.d("${this.javaClass.simpleName}: Acción recibida - tipo: ${action.type}, datos: ${action.data}")
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
            val dialog = super.onCreateDialog(savedInstanceState)
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
}
}
}