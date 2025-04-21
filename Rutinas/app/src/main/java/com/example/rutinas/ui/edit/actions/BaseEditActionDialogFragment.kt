package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import timber.log.Timber


abstract class BaseEditActionDialogFragment : DialogFragment(){
    companion object {
        const val ARG_ACTION = "arg_action"
    }

    protected lateinit var action: Action

    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            action = requireArguments().getParcelable(ARG_ACTION) as? Action
                ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            Timber.d("${this.javaClass.simpleName}: Acción recibida - tipo: ${action.type}, datos: ${action.data}")
        } catch (e: Exception) {
            Timber.e("${this.javaClass.simpleName}: Error al obtener la acción - ${e.message}")
            e.printStackTrace()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }

    protected abstract fun saveAction()
}