package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditAnnouncementActionBinding
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
 class EditAnnouncementActionDialogFragment
     : BaseEditActionDialogFragment(),
       ActionEditorDialog,
       Parcelable {

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        actionUpdateListener = listener
    }

    private var _binding: FragmentEditAnnouncementActionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditAnnouncementActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditAnnouncementActionBinding.inflate(layoutInflater)
        setupUI()

        action = requireArguments().getParcelable<Action>(ARG_ACTION)
            ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

        Timber.d("EditAnnouncementActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

        loadActionData()

        return AlertDialog.Builder(requireContext()).apply {
            setView(binding.root)
            setTitle("Configurar Anuncio")
            setPositiveButton("Guardar") { _, _ ->
                saveAction() }
            setNegativeButton("Cancelar") { _, _ -> dismiss() }
        }.create()
    }

    private fun setupUI() {
        Timber.d("EditAnnouncementActionDialogFragment: setupUI() llamado")
        with(binding) {
            sliderVolume.max = 100
            // Eliminamos el listener del botón ya que ahora usamos los botones del AlertDialog
            btnSave.visibility = View.GONE
        }
    }

    private fun loadActionData() {
        Timber.d("EditAnnouncementActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { data ->
                val message = data["message"] as? String ?: ""
                val volume = (data["volume"] as? Int) ?: 50

                Timber.d("EditAnnouncementActionDialogFragment: Datos cargados - mensaje: $message, volumen: $volume")

                binding.etMessage.setText(message)
                binding.sliderVolume.progress = volume
            }
        } catch (e: Exception) {
            Timber.e("EditAnnouncementActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    private fun saveAction() {
        Timber.d("EditAnnouncementActionDialogFragment: saveAction() llamado")
        try {
            val message = binding.etMessage.text.toString()
            val volume = binding.sliderVolume.progress

            Timber.d("EditAnnouncementActionDialogFragment: Guardando - mensaje: $message, volumen: $volume")

            val updatedAction = action.copy(
                data = mapOf(
                    "message" to message,
                    "volume" to volume
                )
            )

            actionUpdateListener?.invoke(updatedAction)
            Timber.d("EditAnnouncementActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditAnnouncementActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditAnnouncementActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action): EditAnnouncementActionDialogFragment {
            return EditAnnouncementActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}