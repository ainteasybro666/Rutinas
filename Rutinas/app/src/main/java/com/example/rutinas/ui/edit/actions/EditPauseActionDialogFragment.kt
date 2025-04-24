package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditPauseActionBinding
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class EditPauseActionDialogFragment : BaseEditActionDialogFragment(), ActionEditorDialog {
    private var _binding: FragmentEditPauseActionBinding? = null
    private val binding get() = _binding!!

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        actionUpdateListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentEditPauseActionBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding.root)

        builder.setPositiveButton("Guardar") { _, _ ->
            Timber.d("EditPauseActionDialogFragment: Botón Guardar pulsado")
            saveAction()
        }
        builder.setNegativeButton("Cancelar") { _, _ ->
            Timber.d("EditPauseActionDialogFragment: Botón Cancelar pulsado")
        }

        setupUI()
        loadActionData()

        return try{
            builder.create()
        } catch (e: Exception){
            Timber.e("Error creating dialog - ${e.message}")
            dismiss()
            // Mostrar SnackBar
            Snackbar.make(binding.root, "Error al cargar la acción", Snackbar.LENGTH_LONG).show()
            AlertDialog.Builder(requireContext()).setTitle("Error").setMessage("Failed to load action")
                .setPositiveButton("OK", null)
                .create()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadActionData()
    }

    private fun setupUI() {
        Timber.d("EditPauseActionDialogFragment: setupUI() llamado")
        with(binding) {
            // Configurar el botón "Guardar"
            // ya no es necesario. Se ha movido la lógica a onCreateDialog
            //(dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            //    saveAction()
            //}

            //(dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            //    Timber.d("EditPauseActionDialogFragment: Botón Volver pulsado")
            //    dismiss()
            //}
        }
    }

    private fun loadActionData() {
        Timber.d("EditPauseActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { data ->
                val duration = data["duration"] as? Long

                Timber.d("EditPauseActionDialogFragment: Datos cargados - duración: $duration")

                binding.durationEditText.setText(duration?.div(1000).toString())
            }
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al cargar datos - ${e.message}")
            // Mostrar SnackBar
            Snackbar.make(binding.root, "Error al cargar la acción", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun saveAction() {
        Timber.d("EditPauseActionDialogFragment: saveAction() llamado")
        try {
            val durationString = binding.durationEditText.text.toString()
            if (durationString.isEmpty()) {
                // Mostrar SnackBar
                Snackbar.make(binding.root, "La duración no puede estar vacía", Snackbar.LENGTH_LONG).show()
                return
            }

            val duration = try {
                durationString.toLong() * 1000
            } catch (e: NumberFormatException) {
                // Mostrar SnackBar
                Snackbar.make(binding.root, "Duración inválida", Snackbar.LENGTH_LONG).show()
                return
            }

            Timber.d("EditPauseActionDialogFragment: Guardando - duración: $duration")

            val updatedAction = action.copy(
                data = mapOf(
                    "duration" to duration
                ),
            )
            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditPauseActionDialogFragment: Acción actualizada y notificada")
            dismiss()
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al guardar acción - ${e.message}")
            // Mostrar SnackBar
            Snackbar.make(binding.root, "Error al guardar la acción", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        Timber.d("EditPauseActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACTION = "arg_action"
        fun newInstance(action: Action) = EditPauseActionDialogFragment().apply { arguments = Bundle().apply { putParcelable(ARG_ACTION, action) } }
    }
}
