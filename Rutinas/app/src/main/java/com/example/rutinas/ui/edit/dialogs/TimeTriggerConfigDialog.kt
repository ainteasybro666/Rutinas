package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import androidx.preference.PreferenceFragmentCompat
import com.example.rutinas.R
import com.example.rutinas.data.model.Trigger
import timber.log.Timber
import java.util.Calendar
import java.util.UUID

class TimeTriggerConfigDialog : DialogFragment() {

    interface TimeTriggerConfigListener {
        fun onTimeTriggerConfigured(trigger: Trigger)
    }

    private var listener: TimeTriggerConfigListener? = null

    fun setTimeTriggerConfigListener(listener: TimeTriggerConfigListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_trigger_config, null)

        val timePicker = view.findViewById<TimePicker>(R.id.timePicker).apply {
            setIs24HourView(true)
            val cal = Calendar.getInstance()
            hour = cal.get(Calendar.HOUR_OF_DAY)
            minute = cal.get(Calendar.MINUTE)
        }

        val frequencyRadioGroup = view.findViewById<RadioGroup>(R.id.frequencyRadioGroup)
        val weeklyContainer = view.findViewById<FrameLayout>(R.id.weeklyContainer)
        val dayOfMonthPicker = view.findViewById<NumberPicker>(R.id.dayOfMonthPicker)

        dayOfMonthPicker.minValue = 1
        dayOfMonthPicker.maxValue = 31
        dayOfMonthPicker.value = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        frequencyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.weeklyRadioButton -> {
                    weeklyContainer.visibility = View.VISIBLE
                    dayOfMonthPicker.visibility = View.GONE
                    showWeeklyPreferenceFragment()
                }
                R.id.monthlyRadioButton -> {
                    weeklyContainer.visibility = View.GONE
                    dayOfMonthPicker.visibility = View.VISIBLE
                }
                else -> {
                    weeklyContainer.visibility = View.GONE
                    dayOfMonthPicker.visibility = View.GONE
                }
            }
        }

        weeklyContainer.visibility = View.GONE
        dayOfMonthPicker.visibility = View.GONE

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Configurar Trigger de Tiempo")
            .setPositiveButton("Aceptar") { _, _ ->
                val hour = timePicker.hour
                val minute = timePicker.minute
                val frequency = when (frequencyRadioGroup.checkedRadioButtonId) {
                    R.id.dailyRadioButton   -> "daily"
                    R.id.weeklyRadioButton  -> "weekly"
                    R.id.monthlyRadioButton -> "monthly"
                    else                    -> "daily"
                }

                val daysOfWeek = getSelectedDaysOfWeek()
                val dayOfMonth = dayOfMonthPicker.value

                val configData = mutableMapOf<String, Any>(
                    "hour" to hour,
                    "minute" to minute,
                    "frequency" to frequency
                )

                if (frequency == "weekly") {
                    // Serializamos como ArrayList para que sea Serializable
                    configData["daysOfWeek"] = ArrayList(daysOfWeek)
                } else if (frequency == "monthly") {
                    configData["dayOfMonth"] = dayOfMonth
                }

                Timber.d("ConfigData para el trigger: $configData")

                val trigger = Trigger(
                    routineId   = 0,
                    uuid        = UUID.randomUUID().toString(),
                    triggerType = "TIME",
                    data        = configData
                )

                listener?.onTimeTriggerConfigured(trigger)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun showWeeklyPreferenceFragment() {
        childFragmentManager.commit {
            replace(R.id.weeklyContainer, WeeklyPreferenceFragment())
        }
    }

    private fun getSelectedDaysOfWeek(): List<Int> {
        val frag = childFragmentManager.findFragmentById(R.id.weeklyContainer)
                as? WeeklyPreferenceFragment
        return frag?.getSelectedDays() ?: emptyList()
    }

    class WeeklyPreferenceFragment : PreferenceFragmentCompat() {
        private val selectedDays = mutableSetOf<String>()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.weekly_preferences, rootKey)
            findPreference<MultiSelectListPreference>("days_of_week")?.setOnPreferenceChangeListener { _, newValue ->
                selectedDays.clear()
                selectedDays.addAll(newValue as? Set<String> ?: emptySet())
                true
            }
        }

        fun getSelectedDays(): List<Int> =
            selectedDays.mapNotNull { it.toIntOrNull() }
    }
}
