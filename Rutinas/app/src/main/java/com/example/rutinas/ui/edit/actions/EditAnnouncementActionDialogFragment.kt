package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditAnnouncementActionBinding
import timber.log.Timber

class EditAnnouncementActionDialogFragment : BaseEditActionDialogFragment(), ActionEditorDialog {
    private lateinit var action: Action
    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditAnnouncementActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditAnnouncementActionBinding.inflate(layoutInflater)

        try {
            action = requireArguments().getParcelable<Action>(ARG_ACTION)
                ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

            Timber.d("EditAnnouncementActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

            setupUI()
            loadActionData()
        } catch (e: Exception) {
            Timber.e("EditAnnouncementActionDialogFragment: Error al obtener la acción - ${e.message}")
            e.printStackTrace()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("Configurar Anuncio")
            .setPositiveButton("Guardar") { _, _ ->
                Timber.d("EditAnnouncementActionDialogFragment: Botón Guardar pulsado")
                saveAction()
            }

        return super.onCreateDialog(savedInstanceState)
    }

            .setNegativeButton("Cancelar") { _, _ ->
                Timber.d("EditAnnouncementActionDialogFragment: Botón Cancelar pulsado")
                dismiss()
            }
            .create()
    }

    private fun setupUI() {
        Timber.d("EditAnnouncementActionDialogFragment: setupUI() llamado")
        with(binding) {
            sliderVolume.max = 100
            sliderVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    Timber.d("EditAnnouncementActionDialogFragment: Volumen cambiado a $progress")
                    // Actualizar el volumen
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            etMessage.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    Timber.d("EditAnnouncementActionDialogFragment: Mensaje cambiado a ${s.toString()}")
                    // Actualizar el mensaje
                }
            })

            // Eliminamos el listener del botón ya que ahora usamos los botones del AlertDialog
            binding.btnSave.visibility = View.GONE
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

            onActionUpdatedListener?.invoke(updatedAction)
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