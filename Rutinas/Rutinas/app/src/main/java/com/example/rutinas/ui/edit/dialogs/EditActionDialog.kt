package com.example.rutinas.ui.edit.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.databinding.DialogEditActionBinding
import com.example.rutinas.ui.edit.actions.BaseEditActionDialogFragment.Companion.ARG_ACTION
import timber.log.Timber

class EditActionDialog : DialogFragment() {
    private var _binding: DialogEditActionBinding? = null
    private val binding get() = _binding!!
    private var onActionUpdatedListener: ((Action) -> Unit)? = null
    private lateinit var action: Action

    fun setOnActionUpdatedListener(listener: (Action) -> Unit) {
        onActionUpdatedListener = listener
    }

    /* Funciones de extensión para conversión segura */
    private fun Any?.toIntSafe(default: Int = 0): Int {
        return when (this) {
            is String -> this.toIntOrNull() ?: default
            is Number -> this.toInt()
            else -> default
        }
    }

    private fun Any?.toBooleanSafe(default: Boolean = false): Boolean {
        return when (this) {
            is Boolean -> this
            is String -> this.toBoolean()
            else -> default
        }
    }

    private fun setupAlarmView() {
        with(binding) {
            action.data?.let { data ->
                timePicker.hour = data["hour"].toIntSafe(8)
                timePicker.minute = data["minute"].toIntSafe(0)
                etAlarmLabel.setText((data["label"] as? String) ?: "")
            } ?: run {
                // Valores por defecto
                timePicker.hour = 8
                timePicker.minute = 0
                etAlarmLabel.setText("")
            }
        }
    }

    private fun setupAnnouncementView() {
        with(binding) {
            action.data?.let { data ->
                etAnnouncementMessage.setText((data["message"] as? String) ?: "")
                sliderVolume.progress = data["volume"].toIntSafe(50)
            } ?: run {
                etAnnouncementMessage.setText("")
                sliderVolume.progress = 50
            }
            sliderVolume.max = 100
        }
    }

    private fun setupBrightnessView() {
        with(binding) {
            action.data?.let { data ->
                sliderBrightness.progress = data["brightness"].toIntSafe(50)
                switchAutomatic.isChecked = data["isAutomatic"].toBooleanSafe(false)
            } ?: run {
                sliderBrightness.progress = 50
                switchAutomatic.isChecked = false
            }
            sliderBrightness.max = 100
            switchAutomatic.setOnCheckedChangeListener { _, isChecked ->
                sliderBrightness.isEnabled = !isChecked
            }
        }
    }

    private fun setupVolumeView() {
        with(binding) {
            action.data?.let { data ->
                sliderVolume.progress = data["volume"].toIntSafe(50)
                when ((data["streamType"] as? String)) {
                    "ringtone" -> rbRingtone.isChecked = true
                    "media" -> rbMedia.isChecked = true
                    "alarm" -> rbAlarm.isChecked = true
                    else -> rbRingtone.isChecked = true
                }
            } ?: run {
                sliderVolume.progress = 50
                rbRingtone.isChecked = true
            }
            sliderVolume.max = 100
        }
    }

    private fun setupSoundModeView() {
        with(binding) {
            action.data?.let { data ->
                when ((data["mode"] as? String)) {
                    "normal" -> rbNormal.isChecked = true
                    "silent" -> rbSilent.isChecked = true
                    "vibrate" -> rbVibrate.isChecked = true
                    else -> rbNormal.isChecked = true
                }
            } ?: run {
                rbNormal.isChecked = true
            }
        }
    }

    private fun setupTimeView() {
        with(binding) {
            npHours.apply {
                minValue = 0
                maxValue = 23
                value = action.data?.get("hours").toIntSafe(0)
            }

            npMinutes.apply {
                minValue = 0
                maxValue = 59
                value = action.data?.get("minutes").toIntSafe(0)
            }

            npSeconds.apply {
                minValue = 0
                maxValue = 59
                value = action.data?.get("seconds").toIntSafe(0)
            }
        }
    }

    // Actualizar el método onCreateDialog para usar estos métodos de configuración
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditActionBinding.inflate(layoutInflater)
        action = arguments?.getSerializable(ARG_ACTION) as Action
            ?: throw IllegalArgumentException("No se pudo obtener la acción para editar")
        Timber.d("Editando acción: ${action.type}, datos: ${action.data}")

        // Ocultar todas las vistas primero
        hideAllViews()

        // Mostrar y configurar la vista correspondiente
        when (action.type) {
            ActionType.ALARM.name -> {
                binding.layoutAlarm.visibility = View.VISIBLE
                setupAlarmView()
            }
            ActionType.ANNOUNCEMENT.name -> {
                binding.layoutAnnouncement.visibility = View.VISIBLE
                setupAnnouncementView()
            }
            ActionType.BRIGHTNESS.name -> {
                binding.layoutBrightness.visibility = View.VISIBLE
                setupBrightnessView()
            }
            ActionType.VOLUME.name -> {
                binding.layoutVolume.visibility = View.VISIBLE
                setupVolumeView()
            }
            ActionType.SOUND_MODE.name -> {
                binding.radioGroupSoundMode.visibility = View.VISIBLE
                setupSoundModeView()
            }
            ActionType.TIME.name -> {
                binding.layoutTime.visibility = View.VISIBLE
                setupTimeView()
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(getActionTitle())
            .setView(binding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val updatedAction = getUpdatedAction()
                onActionUpdatedListener?.invoke(updatedAction)
                dismiss()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                dismiss()
            }
            .create()
    }

    private fun hideAllViews() {
        with(binding) {
            layoutAlarm.visibility = View.GONE
            layoutAnnouncement.visibility = View.GONE
            layoutBrightness.visibility = View.GONE
            layoutVolume.visibility = View.GONE
            radioGroupSoundMode.visibility = View.GONE
            layoutTime.visibility = View.GONE
        }
    }

    fun getActionTitle(): String {
        return when (action.type) {
            ActionType.ALARM.name -> "Configurar Alarma"
            ActionType.ANNOUNCEMENT.name -> "Editar Anuncio"
            ActionType.BRIGHTNESS.name -> "Ajustar Brillo"
            ActionType.VOLUME.name -> "Ajustar Volumen"
            ActionType.SOUND_MODE.name -> "Configurar Sonido"
            ActionType.TIME.name -> "Configurar Tiempo"
            else -> "Editar Acción"
        }
    }

    private fun getUpdatedAction(): Action {
        return when (action.type) {
            ActionType.ALARM.name -> {
                val hour = binding.timePicker.hour
                val minute = binding.timePicker.minute
                val label = binding.etAlarmLabel.text.toString()

                action.copy(
                    data = mapOf(
                        "hour" to hour,
                        "minute" to minute,
                        "label" to label,
                        "isEnabled" to true
                    )
                )
            }
            ActionType.ANNOUNCEMENT.name -> {
                val message = binding.etAnnouncementMessage.text.toString()
                val volume = binding.sliderVolume.progress

                action.copy(
                    data = mapOf(
                        "message" to message,
                        "volume" to volume
                    )
                )
            }
            ActionType.BRIGHTNESS.name -> {
                val brightness = binding.sliderBrightness.progress

                action.copy(
                    data = mapOf(
                        "brightness" to brightness,
                        "isAutomatic" to binding.switchAutomatic.isChecked
                    )
                )
            }
            ActionType.VOLUME.name -> {
                val volume = binding.sliderVolume.progress
                val streamType = when (binding.radioGroupStream.checkedRadioButtonId) {
                    R.id.rbRingtone -> "ringtone"
                    R.id.rbMedia -> "media"
                    R.id.rbAlarm -> "alarm"
                    else -> "system"
                }

                action.copy(
                    data = mapOf(
                        "volume" to volume,
                        "streamType" to streamType
                    )
                )
            }
            ActionType.SOUND_MODE.name -> {
                val soundMode = when (binding.radioGroupSoundMode.checkedRadioButtonId) {
                    R.id.rbNormal -> "normal"
                    R.id.rbSilent -> "silent"
                    R.id.rbVibrate -> "vibrate"
                    else -> "normal"
                }

                action.copy(
                    data = mapOf(
                        "mode" to soundMode
                    )
                )
            }
            ActionType.TIME.name -> {
                val hours = binding.npHours.value
                val minutes = binding.npMinutes.value
                val seconds = binding.npSeconds.value

                action.copy(
                    data = mapOf(
                        "hours" to hours,
                        "minutes" to minutes,
                        "seconds" to seconds
                    )
                )
            }
            else -> action
        }
    }

    companion object {
        private const val ARG_ACTION = "arg_action"

        fun newInstance(action: Action) = EditActionDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_ACTION, action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}