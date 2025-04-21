package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.rutinas.R
import com.example.rutinas.data.model.Trigger
import java.util.UUID
import java.util.Calendar

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

        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
        timePicker.setIs24HourView(true) // Use 24-hour format

        val frequencyRadioGroup = view.findViewById<RadioGroup>(R.id.frequencyRadioGroup)
        val weeklyContainer = view.findViewById<FrameLayout>(R.id.weeklyContainer)
        val dayOfMonthPicker = view.findViewById<NumberPicker>(R.id.dayOfMonthPicker)

        // Initialize TimePicker with current time
        val calendar = Calendar.getInstance()
        timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
        timePicker.minute = calendar.get(Calendar.MINUTE)

        // Set up NumberPicker for day of month
        dayOfMonthPicker.minValue = 1
        dayOfMonthPicker.maxValue = 31

        // Show WeeklyPreferenceFragment when "Weekly" is selected
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

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Configurar Trigger de Tiempo")
            .setPositiveButton("Aceptar") { _, _ ->
                val hour = timePicker.hour
                val minute = timePicker.minute
                val frequency = when (frequencyRadioGroup.checkedRadioButtonId) {
                    R.id.dailyRadioButton -> "daily"
                    R.id.weeklyRadioButton -> "weekly"
                    R.id.monthlyRadioButton -> "monthly"
                    else -> "daily" // Default to daily
                }

                val daysOfWeek = getSelectedDaysOfWeek()
                val dayOfMonth = dayOfMonthPicker.value

                val configData = mapOf(
                    "frequency" to frequency,
                    "daysOfWeek" to daysOfWeek,
                    "dayOfMonth" to dayOfMonth
                )

                val trigger = Trigger(
                    routineId = 0,
                    uuid = UUID.randomUUID().toString(),
                    triggerType = "TIME",
                    hour = hour,
                    minute = minute,
                    data = configData,
                    dayOfWeek = daysOfWeek
                )//  Remove unused fields
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
        val fragment =
            childFragmentManager.findFragmentById(R.id.weeklyContainer) as? WeeklyPreferenceFragment
        return fragment?.getSelectedDays() ?: emptyList()
    }

    // PreferenceFragment for MultiSelectListPreference
    class WeeklyPreferenceFragment : PreferenceFragmentCompat() {
        private val selectedDays = mutableSetOf<String>()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.weekly_preferences, rootKey)

            findPreference<MultiSelectListPreference>("days_of_week")?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    selectedDays.clear()
                    selectedDays.addAll(newValue as? Set<String> ?: emptySet())
                    true
                }
            }
        }

        fun getSelectedDays(): List<Int> {
            return selectedDays.mapNotNull { it.toIntOrNull() }
        }
    }
}
