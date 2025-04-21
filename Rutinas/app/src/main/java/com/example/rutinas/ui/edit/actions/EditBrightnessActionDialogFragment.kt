package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.FragmentEditBrightnessActionBinding
import kotlinx.parcelize.Parcelize
import timber.log.Timber

class EditBrightnessActionDialogFragment : BaseEditActionDialogFragment(), ActionEditorDialog {
    private var _binding: FragmentEditBrightnessActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action
    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as AlertDialog
        _binding = FragmentEditBrightnessActionBinding.inflate(layoutInflater)

        try {
            action = requireArguments().getParcelable<Action>(ARG_ACTION)
                ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")

            Timber.d("EditBrightnessActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

            setupUI()
            loadActionData()
        } catch (e: Exception) {
            return showErrorDialog(e.message ?: "Unknown error")
        }

        dialog.setView(binding.root)
        dialog.setTitle("Ajustar Brillo")
        dialog.setButton(
            DialogInterface.BUTTON_POSITIVE, "Guardar"
        ) { _, _ ->
            Timber.d("EditBrightnessActionDialogFragment: Botón Guardar pulsado")
            saveAction()
        }
        dialog.setButton(
            DialogInterface.BUTTON_NEGATIVE, "Cancelar"
        ) { _, _ ->
            Timber.d("EditBrightnessActionDialogFragment: Botón Cancelar pulsado")
            dismiss()
        }

        return dialog
    }

    private fun setupUI() {
        Timber.d("EditBrightnessActionDialogFragment: setupUI() llamado")
        with(binding) {
            seekBarBrightness.max = 100
            seekBarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    Timber.d("EditBrightnessActionDialogFragment: Brillo cambiado a $progress%")
                    tvBrightnessValue.text = "Brillo: $progress%"
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        }
    }

    private fun loadActionData() {
        Timber.d("EditBrightnessActionDialogFragment: loadActionData() llamado")
        try {
            action.data.let { data ->
                val brightness = (data["brightness"] as? Int) ?: 50

                Timber.d("EditBrightnessActionDialogFragment: Datos cargados - brillo: $brightness%")

                binding.seekBarBrightness.progress = brightness
                binding.tvBrightnessValue.text = "Brillo: $brightness%"
            }
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    private fun saveAction() {
        Timber.d("EditBrightnessActionDialogFragment: saveAction() llamado")
        try {
            val brightness = binding.seekBarBrightness.progress

            Timber.d("EditBrightnessActionDialogFragment: Guardando - brillo: $brightness%")

            val updatedAction = action.copy(
                data = mapOf(
                    "brightness" to brightness
                )
            )

            onActionUpdatedListener?.invoke(updatedAction)
            Timber.d("EditBrightnessActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditBrightnessActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditBrightnessActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action): EditBrightnessActionDialogFragment {
            return EditBrightnessActionDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}