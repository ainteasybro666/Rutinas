package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditPauseActionBinding
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

        return builder.create()
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
        }
    }

    private fun saveAction() {
        Timber.d("EditPauseActionDialogFragment: saveAction() llamado")
        try {
            val duration = binding.durationEditText.text.toString().toLong() * 1000

            Timber.d("EditPauseActionDialogFragment: Guardando - duración: $duration")

            val updatedAction = action.copy(
                data = mapOf(
                    "duration" to duration
                ),
            )
            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditPauseActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al guardar acción - ${e.message}")
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
