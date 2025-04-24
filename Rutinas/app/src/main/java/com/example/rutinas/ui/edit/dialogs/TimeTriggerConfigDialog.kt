package com.example.rutinas.ui.edit.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import androidx.core.view.children
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
        val daysOfWeek = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        val checkboxContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            id = View.generateViewId()
        }

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
                    // Check if checkboxes already exist in the container
                    if (weeklyContainer.findViewById<LinearLayout>(checkboxContainer.id) == null) {
                        daysOfWeek.forEachIndexed { index, day ->
                            val checkBox = CheckBox(context).apply {
                                text = day
                                tag = index + 1 // Use 1-based index to represent day of week
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT)
                            }
                            checkboxContainer.addView(checkBox)
                        }
                        weeklyContainer.addView(checkboxContainer)}
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
        // Initially hide the weekly checkboxes
        dayOfMonthPicker.visibility = View.GONE

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Configurar Trigger de Tiempo")
            .setPositiveButton("Aceptar") { _, _ ->
                // Mover la creación del trigger ANTES de notificar al listener
                val hour = timePicker.hour
                val minute = timePicker.minute
                val frequency = when (frequencyRadioGroup.checkedRadioButtonId) {
                    R.id.dailyRadioButton   -> "daily"
                    R.id.weeklyRadioButton  -> "weekly"
                    R.id.monthlyRadioButton -> "monthly"
                    else                    -> "daily"
                }

                val daysOfWeek = getSelectedDaysOfWeek(weeklyContainer)
                val dayOfMonth = dayOfMonthPicker.value

                val configData = mutableMapOf<String, Any>(
                    "hour" to hour,
                    "minute" to minute,
                    "frequency" to frequency
                ).apply {
                    if (frequency == "weekly") put("daysOfWeek", ArrayList(daysOfWeek))
                    if (frequency == "monthly") put("dayOfMonth", dayOfMonth)
                }

                val trigger = Trigger(
                    routineId = 0,
                    uuid = UUID.randomUUID().toString(),
                    triggerType = "TIME",
                    data = configData
                )

                Timber.d("Enviando trigger configurado: ${trigger.uuid}")
                listener?.onTimeTriggerConfigured(trigger) ?: Timber.e("Listener es null!")
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun showWeeklyCheckboxes(weeklyContainer: FrameLayout) {
        val daysOfWeek = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        val checkboxContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        daysOfWeek.forEachIndexed { index, day ->
            val checkBox = CheckBox(context).apply {
                text = day
                tag = index + 1 // Use 1-based index to represent day of week
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            checkboxContainer.addView(checkBox)
        }

        weeklyContainer.addView(checkboxContainer)
    }

    private fun getSelectedDaysOfWeek(weeklyContainer: FrameLayout): List<Int> {
        val checkboxContainer = weeklyContainer.getChildAt(0) as? LinearLayout
        return checkboxContainer?.children?.mapNotNull {
            val checkbox = it as? CheckBox
            if (checkbox?.isChecked == true) checkbox.tag as? Int else null
        }?.toList() ?: emptyList()
    }

    companion object {
        init { Timber.d("TimeTriggerConfigDialog loaded") }
        fun createInstance(): TimeTriggerConfigDialog {
            return TimeTriggerConfigDialog()
        }
    }
}
