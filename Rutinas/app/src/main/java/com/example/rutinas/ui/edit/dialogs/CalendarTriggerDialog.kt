package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.DialogCalendarTriggerConfigBinding
import timber.log.Timber
import java.util.Calendar

class CalendarTriggerDialog : DialogFragment() {
    interface CalendarTriggerListener {
        fun onCalendarTriggerConfigured(trigger: Trigger)
    }

    private var _binding: DialogCalendarTriggerConfigBinding? = null
    private val binding get() = _binding!!
    private var calendarTriggerListener: CalendarTriggerListener? = null

    private var routineId: Long = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("CalendarTriggerDialog: onCreateDialog() llamado")
        _binding = DialogCalendarTriggerConfigBinding.inflate(LayoutInflater.from(context))

        routineId = arguments?.getLong("routineId") ?: 0

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        binding.datePicker.init(currentYear, currentMonth, currentDay, null)

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Aceptar") { _, _ -> configureCalendarTrigger() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
    }

    private fun configureCalendarTrigger() {
        Timber.d("CalendarTriggerDialog: configureCalendarTrigger() llamado")
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        val trigger = Trigger(
            routineId = routineId,
            triggerType = "CALENDAR",
            data = DataWrapper(
                mapOf(
                    "day" to day,
                    "month" to month,
                    "year" to year
                )
            )
        )
        calendarTriggerListener?.onCalendarTriggerConfigured(trigger)
    }

    fun setCalendarTriggerListener(listener: CalendarTriggerListener) {
        this.calendarTriggerListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun createInstance(routineId: Long): CalendarTriggerDialog {
            val dialog = CalendarTriggerDialog()
            val args = Bundle()
            args.putLong("routineId", routineId)
            dialog.arguments = args
            return dialog
        }
    }
}
