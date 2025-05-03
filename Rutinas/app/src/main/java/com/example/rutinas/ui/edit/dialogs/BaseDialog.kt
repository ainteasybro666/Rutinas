package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.example.rutinas.databinding.DialogBaseBinding

abstract class BaseDialog : DialogFragment() {

    private var _binding: ViewBinding? = null
    protected abstract val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> ViewBinding

    protected val binding
        get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return try {
            val dialog = super.onCreateDialog(savedInstanceState)
            dialog
        } catch (e: Exception) {
            AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage("No se pudo cargar la configuración: ${e.message}")
                .setPositiveButton("Aceptar") { _, _ ->
                    dismiss()
                }.create()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = bindingInflater.invoke(inflater, container, false)
        return binding.root
    }
    fun showDialog(dialog: DialogFragment){
        dialog.show(parentFragmentManager, dialog::class.java.simpleName)
    }
}
