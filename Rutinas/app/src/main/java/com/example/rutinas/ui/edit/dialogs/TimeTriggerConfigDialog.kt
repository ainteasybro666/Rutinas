package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.R
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.DialogTimeTriggerConfigBinding
import timber.log.Timber

class TimeTriggerConfigDialog : DialogFragment() {
    interface TimeTriggerConfigListener {
        fun onTimeTriggerConfigured(trigger: Trigger)
    }

    private var _binding: DialogTimeTriggerConfigBinding? = null
    private val binding get() = _binding!!
    private var timeTriggerConfigListener: TimeTriggerConfigListener? = null

    private var routineId: Long = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("TimeTriggerConfigDialog: onCreateDialog() llamado")
        _binding = DialogTimeTriggerConfigBinding.inflate(LayoutInflater.from(context))
        setupUI()

        routineId = arguments?.getLong("routineId") ?: 0

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Aceptar") { _, _ -> configureTimeTrigger() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
    }

    private fun setupUI() {
        val rgFrequency = binding.rgFrequency
        val rbOnce = binding.rbOnce
        val rbWeekly = binding.rbWeekly
        val rbMonthly = binding.rbMonthly
        val daysOfWeekGroup = binding.daysOfWeekGroup
        val dayOfMonth = binding.dayOfMonth

        // Listener para el RadioGroup
        rgFrequency.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                rbOnce.id -> {
                    daysOfWeekGroup.visibility = View.GONE
                    dayOfMonth.visibility = View.GONE
                }
                rbWeekly.id -> {
                    daysOfWeekGroup.visibility = View.VISIBLE
                    dayOfMonth.visibility = View.GONE
                }
                rbMonthly.id -> {
                    daysOfWeekGroup.visibility = View.GONE
                    dayOfMonth.visibility = View.VISIBLE
                    dayOfMonth.minValue = 1
                    dayOfMonth.maxValue = 31
                    dayOfMonth.value = 1
                }
            }
        }
        rbOnce.isChecked = true // Set default checked radio button
    }

    private fun configureTimeTrigger() {
        Timber.d("TimeTriggerConfigDialog: configureTimeTrigger() llamado")
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val frequency = when (binding.rgFrequency.checkedRadioButtonId) {
            binding.rbWeekly.id -> "weekly"
            binding.rbMonthly.id -> "monthly"
            else -> "once" // Default to "once" if no other option is selected
        }
        val daysOfWeek = if (frequency == "weekly") {
            listOf(
                binding.cbSunday.isChecked,
                binding.cbMonday.isChecked,
                binding.cbTuesday.isChecked,
                binding.cbWednesday.isChecked,
                binding.cbThursday.isChecked,
                binding.cbFriday.isChecked,
                binding.cbSaturday.isChecked
            ).mapIndexedNotNull { index, isChecked -> if (isChecked) index else null }
        } else {
            emptyList()
        }
        val dayOfMonth = if (frequency == "monthly") {
            binding.dayOfMonth.value
        } else {
            1 // Default to 1 if not monthly
        }

        val trigger = Trigger(
            routineId = routineId,
            triggerType = "TIME",
            data = DataWrapper(
                mapOf(
                    "hour" to hour,
                    "minute" to minute,
                    "frequency" to frequency,
                    "daysOfWeek" to daysOfWeek,
                    "dayOfMonth" to dayOfMonth
                )
            )
        )
        timeTriggerConfigListener?.onTimeTriggerConfigured(trigger)
    }

    fun setTimeTriggerConfigListener(listener: TimeTriggerConfigListener) {
        this.timeTriggerConfigListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun createInstance(routineId: Long): TimeTriggerConfigDialog {
            val dialog = TimeTriggerConfigDialog()
            val args = Bundle()
            args.putLong("routineId", routineId)
            dialog.arguments = args
            return dialog
        }
    }
}
