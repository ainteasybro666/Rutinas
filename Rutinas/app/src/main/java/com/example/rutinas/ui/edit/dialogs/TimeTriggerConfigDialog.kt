package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("TimeTriggerConfigDialog: onCreateDialog() llamado")
        _binding = DialogTimeTriggerConfigBinding.inflate(LayoutInflater.from(context))
        setupUI()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Aceptar") { _, _ -> configureTimeTrigger() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
    }

    private fun setupUI() {
        binding.checkboxWeekly.setOnCheckedChangeListener { _, isChecked ->
            binding.daysOfWeekGroup.visibility = if (isChecked) ViewGroup.VISIBLE else ViewGroup.GONE
        }
        binding.checkboxMonthly.setOnCheckedChangeListener { _, isChecked ->
            binding.dayOfMonth.visibility = if (isChecked) ViewGroup.VISIBLE else ViewGroup.GONE
        }
    }
    private fun configureTimeTrigger() {
        Timber.d("TimeTriggerConfigDialog: configureTimeTrigger() llamado")
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val frequency = when {
            binding.checkboxWeekly.isChecked -> "weekly"
            binding.checkboxMonthly.isChecked -> "monthly"
            else -> "daily"
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
            1
        }

        val trigger = Trigger(
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
        fun createInstance(): TimeTriggerConfigDialog {
            return TimeTriggerConfigDialog()
        }
    }
}
