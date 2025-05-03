package com.example.rutinas.ui.edit.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditVolumeActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

class EditVolumeActionDialogFragment(listener: ActionDialogListener) : BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditVolumeActionBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ACTION = "arg_action"
        fun newInstance(action: Action, listener: ActionDialogListener): EditVolumeActionDialogFragment {
            val fragment = EditVolumeActionDialogFragment(listener)
            fragment.arguments = bundleOf(ARG_ACTION to action)
            return fragment
        }
    }
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding {
        _binding = FragmentEditVolumeActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("EditVolumeActionDialogFragment: onViewCreated() llamado")
        super.onViewCreated(view, savedInstanceState)

        try {
            Timber.d("EditVolumeActionDialogFragment: Acción recibida - tipo: ${action.actionType}, datos: ${action.data}")
            setupUI()
            loadActionData()

        } catch (e: Exception) {
            Timber.e("EditVolumeActionDialogFragment: Error en onViewCreated - ${e.message}")
            e.printStackTrace()
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

    override fun saveAction() {
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
                actionType = ActionType.VOLUME,
                data = DataWrapper(volumeData)
            )
            notifyActionUpdated(updatedAction)

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
}
