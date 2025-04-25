package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditVolumeActionBinding
import com.example.rutinas.ui.edit.RoutineEditFragment
import timber.log.Timber


class EditVolumeActionDialogFragment(listener: RoutineEditFragment.ActionDialogListener) : BaseEditActionDialogFragment(listener), ActionEditorDialog {
    private var _binding: FragmentEditVolumeActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var action: Action
    private var onActionUpdatedListener: ((Action) -> Unit)? = null

    override fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("EditVolumeActionDialogFragment: onCreateDialog() llamado")
        super.onCreateDialog(savedInstanceState)
        _binding = FragmentEditVolumeActionBinding.inflate(layoutInflater)

        try {
            
            
            Timber.d("EditVolumeActionDialogFragment: Acción recibida - tipo: ${action.type}, datos: ${action.data}")

            setupUI()
            loadActionData()

            val dialog =  super.onCreateDialog(savedInstanceState) as AlertDialog
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Ajustar Volumen")
            builder.setView(binding.root)
            builder.setPositiveButton("Guardar") { _, _ ->
                Timber.d("EditVolumeActionDialogFragment: Botón Guardar pulsado")
                saveAction()
            }
            builder.setNegativeButton("Cancelar") { _, _ ->
                Timber.d("EditVolumeActionDialogFragment: Botón Cancelar pulsado")
            }
            return builder.create()


        } catch (e: Exception) {
            Timber.e("EditVolumeActionDialogFragment: Error en onCreateDialog - ${e.message}")
            e.printStackTrace()

            // Crear un diálogo de error en caso de fallo
            return AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage("No se pudo cargar la acción: ${e.message}")
                .setPositiveButton("Aceptar") { _, _ ->
                    dismiss()
                }
                .create()
        }
    }

    private fun setupUI() {
        Timber.d("EditVolumeActionDialogFragment: setupUI() llamado")
        with(binding) {
            cbVolumeMedia.setOnCheckedChangeListener { _, isChecked ->
                Timber.d("EditVolumeActionDialogFragment: Checkbox Media cambiado a $isChecked")
                seekBarMediaVolume.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            cbVolumeRingtone.setOnCheckedChangeListener { _, isChecked ->
                Timber.d("EditVolumeActionDialogFragment: Checkbox Ringtone cambiado a $isChecked")
                seekBarRingtoneVolume.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            cbVolumeAlarm.setOnCheckedChangeListener { _, isChecked ->
                Timber.d("EditVolumeActionDialogFragment: Checkbox Alarm cambiado a $isChecked")
                seekBarAlarmVolume.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            setupSeekBar(seekBarMediaVolume, tvMediaVolumeValue, "Media")
            setupSeekBar(seekBarRingtoneVolume, tvRingtoneVolumeValue, "Ringtone")
            setupSeekBar(seekBarAlarmVolume, tvAlarmVolumeValue, "Alarm")
        }
    }

    private fun setupSeekBar(seekBar: SeekBar, textView: TextView, name: String) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Timber.d("EditVolumeActionDialogFragment: SeekBar $name cambiado a $progress")
                textView.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadActionData() {
        Timber.d("EditVolumeActionDialogFragment: loadActionData() llamado")
        try {
            action.data?.let { dataWrapper ->
                val data = dataWrapper.data
                
                with(binding) {
                    data["mediaVolume"]?.let { volume ->
                        val mediaVolume = (volume as? Number)?.toInt() ?: 50
                        Timber.d("EditVolumeActionDialogFragment: Volumen Media cargado: $mediaVolume")
                        cbVolumeMedia.isChecked = true
                        seekBarMediaVolume.progress = mediaVolume
                    }
                    data["ringtoneVolume"]?.let { volume ->
                        val ringtoneVolume = (volume as? Number)?.toInt() ?: 50
                        Timber.d("EditVolumeActionDialogFragment: Volumen Ringtone cargado: $ringtoneVolume")
                        cbVolumeRingtone.isChecked = true
                        seekBarRingtoneVolume.progress = ringtoneVolume
                    }
                    data["alarmVolume"]?.let { volume ->
                        val alarmVolume = (volume as? Number)?.toInt() ?: 50
                        Timber.d("EditVolumeActionDialogFragment: Volumen Alarm cargado: $alarmVolume")
                        cbVolumeAlarm.isChecked = true
                        seekBarAlarmVolume.progress = alarmVolume
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("EditVolumeActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    private fun saveAction() {
        Timber.d("EditVolumeActionDialogFragment: saveAction() llamado")
        try {
            val volumeData = mutableMapOf<String, Any>()
            with(binding) {
                if (cbVolumeMedia.isChecked) {
                    volumeData["mediaVolume"] = seekBarMediaVolume.progress
                    Timber.d("EditVolumeActionDialogFragment: Guardando volumen Media: ${seekBarMediaVolume.progress}")
                }
                if (cbVolumeRingtone.isChecked) {
                    volumeData["ringtoneVolume"] = seekBarRingtoneVolume.progress
                    Timber.d("EditVolumeActionDialogFragment: Guardando volumen Ringtone: ${seekBarRingtoneVolume.progress}")
                }
                if (cbVolumeAlarm.isChecked) {
                    volumeData["alarmVolume"] = seekBarAlarmVolume.progress
                    Timber.d("EditVolumeActionDialogFragment: Guardando volumen Alarm: ${seekBarAlarmVolume.progress}")
                }
            }

            val updatedAction = action.copy(
                data = DataWrapper(volumeData)
            )
            
            actionUpdateListener?.invoke(updatedAction)
            listener.onActionUpdated(updatedAction)

            Timber.d("EditVolumeActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditVolumeActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditVolumeActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(
            action: Action,
            listener: RoutineEditFragment.ActionDialogListener
        ): EditVolumeActionDialogFragment {
            return EditVolumeActionDialogFragment(listener).apply {
            arguments = Bundle().apply {
                putParcelable(ARG_ACTION, action)
            }
        }
    }
}