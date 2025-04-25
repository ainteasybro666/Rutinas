package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditAnnouncementActionBinding
import com.example.rutinas.ui.edit.RoutineEditFragment
import timber.log.Timber

class EditAnnouncementActionDialogFragment(listener: RoutineEditFragment.ActionDialogListener)
     : BaseEditActionDialogFragment(listener),
       ActionEditorDialog {


    private var _binding: FragmentEditAnnouncementActionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditAnnouncementActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditAnnouncementActionBinding.inflate(layoutInflater)
        setupUI()


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
            action.data?.let { dataWrapper ->
                val data = dataWrapper.data
                 val message = data["message"] as? String ?: ""
                 val volume = (data["volume"] as? Int) ?: 50

                Timber.d("EditAnnouncementActionDialogFragment: Datos cargados - mensaje: $message, volumen: $volume")

                // Cargar los datos en los elementos de la UI
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
                data = DataWrapper(mapOf(
                    "message" to message,
                    "volume" to volume
                ))
            )

            // Notificar a los listeners que la acción ha sido actualizada
            listener.onActionUpdated(updatedAction)
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

        fun newInstance(action: Action, listener: RoutineEditFragment.ActionDialogListener): EditAnnouncementActionDialogFragment {
            return EditAnnouncementActionDialogFragment(listener).apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}