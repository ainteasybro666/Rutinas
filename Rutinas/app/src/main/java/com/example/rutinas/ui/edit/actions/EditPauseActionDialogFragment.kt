package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditPauseActionBinding
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
class EditPauseActionDialogFragment : DialogFragment(), ActionEditorDialog, Parcelable {
    private var _binding: FragmentEditPauseActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action
    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditPauseActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditPauseActionBinding.inflate(layoutInflater)

        try {
            action = requireArguments().getParcelable(ARG_ACTION)
                ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

            Timber.d("EditPauseActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

            setupUI()
            loadActionData()
        } catch (e: Exception) {
            Timber.e("EditPauseActionDialogFragment: Error al obtener la acción - ${e.message}")
            e.printStackTrace()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        dialog.setOnShowListener {
            Timber.d("EditPauseActionDialogFragment: Diálogo mostrado")
        }

        return dialog
    }

    private fun setupUI() {
        Timber.d("EditPauseActionDialogFragment: setupUI() llamado")
        with(binding) {
            // Configurar el botón "Guardar"
            btnSave.setOnClickListener {
                Timber.d("EditPauseActionDialogFragment: Botón Guardar pulsado")
                saveAction()
                dismiss()
            }

            // Configurar la flecha de retroceso
            btnBack.setOnClickListener {
                Timber.d("EditPauseActionDialogFragment: Botón Volver pulsado")
                dismiss()
            }
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
                pauseDuration = duration
            )
            onActionUpdatedListener?.invoke(updatedAction)
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

        fun newInstance(action: Action): EditPauseActionDialogFragment {
            return EditPauseActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}