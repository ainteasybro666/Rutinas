package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditBrightnessActionBinding
import com.example.rutinas.ui.edit.RoutineEditFragment
import timber.log.Timber

class EditBrightnessActionDialogFragment(private val listener: RoutineEditFragment.ActionDialogListener) :
    BaseEditActionDialogFragment(listener),
    ActionEditorDialog {
    private var _binding: FragmentEditBrightnessActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action






    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        Timber.d("EditBrightnessActionDialogFragment: onCreateDialog() llamado")
        _binding = FragmentEditBrightnessActionBinding.inflate(layoutInflater)

        Timber.d("EditBrightnessActionDialogFragment: Acción recibida - tipo: ${action.actionType}, datos: ${action.data}")

        setupUI()
        loadActionData()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Ajustar Brillo")
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ ->
                Timber.d("EditBrightnessActionDialogFragment: Botón Guardar pulsado")
                saveAction()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Timber.d("EditBrightnessActionDialogFragment: Botón Cancelar pulsado")
                dismiss()
            }
            .create()

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

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })


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
                data = DataWrapper(mapOf(
                    "brightness" to brightness))
            )

            listener.onActionUpdated(updatedAction)

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

        fun newInstance(action: Action, listener: RoutineEditFragment.ActionDialogListener): EditBrightnessActionDialogFragment {
            return EditBrightnessActionDialogFragment(listener).apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ACTION, action)
                }
            }
        }
    }
}