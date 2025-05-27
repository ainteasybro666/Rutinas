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
    private var existingTrigger: Trigger? = null // <<< Add this field >>>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("CalendarTriggerDialog: onCreateDialog() llamado")
        _binding = DialogCalendarTriggerConfigBinding.inflate(LayoutInflater.from(context))

        routineId = arguments?.getLong("routineId") ?: 0
        // <<< Recuperar el trigger existente de los argumentos si existe >>>
         existingTrigger = arguments?.getParcelable("existingTrigger") // Use getParcelable

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        binding.datePicker.init(currentYear, currentMonth, currentDay, null)

        // <<< AÑADIMOS LÓGICA PARA POBLAR LA UI SI ESTAMOS EDITANDO >>>
        existingTrigger?.let { trigger ->
                Timber.d("CalendarTriggerDialog: Editando trigger existente (UUID: ${trigger.uuid}). Poblanco UI...")
            populateUI(trigger) // Call a new function to populate the UI
            } ?: run {
            Timber.d("CalendarTriggerDialog: Creando nuevo trigger. UI con valores por defecto (fecha actual).")
                // If no existing trigger, the UI is already configured with default date
            }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Aceptar") { _, _ -> configureCalendarTrigger() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
    }

    // <<< NUEVA FUNCIÓN PARA POBLAR LA UI DESDE UN TRIGGER EXISTENTE >>>
    private fun populateUI(trigger: Trigger) {
        Timber.d("CalendarTriggerDialog: populateUI() llamado para trigger UUID: ${trigger.uuid}")
        val day = trigger.getInt("day")
        val month = trigger.getInt("month")
        val year = trigger.getInt("year")
        val calendarTime = trigger.getLocalDateTime("calendarTime") // If you save LocalDateTime

        // Poblar DatePicker
        if (year != null && month != null && day != null) {
            // DatePicker months are 0-indexed
            binding.datePicker.updateDate(year, month, day)
            Timber.d("CalendarTriggerDialog: DatePicker set to $day/${month + 1}/$year from trigger.")
            } else {
            Timber.w("CalendarTriggerDialog: Incomplete date data in trigger. DatePicker not set.")
            }

        // If you also save time for calendar triggers, populate your time picker here
        // calendarTime?.let {
        //     binding.timePickerCalendar.hour = it.hour
        //     binding.timePickerCalendar.minute = it.minute
        //     Timber.d("CalendarTriggerDialog: TimePicker set to ${it.toLocalTime()} from trigger.")
        // } ?: run {
        //     Timber.w("CalendarTriggerDialog: Calendar time is null in trigger data.")
        // }

        Timber.d("CalendarTriggerDialog: UI populated with existing trigger data.")
        }

    private fun configureCalendarTrigger() {
        Timber.d("CalendarTriggerDialog: configureCalendarTrigger() llamado")
        val day = binding.datePicker.dayOfMonth
        val month = binding.datePicker.month
        val year = binding.datePicker.year

        val trigger = Trigger(
            routineId = routineId,
            triggerType = TriggerTypeDialog.TriggerType.CALENDAR,
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
        fun createInstance(routineId: Long, existingTrigger: Trigger? = null): CalendarTriggerDialog { // <<< Add optional Trigger parameter >>>
            val dialog = CalendarTriggerDialog()
            val args = Bundle()
            args.putLong("routineId", routineId)
            if (existingTrigger != null) {
                args.putParcelable("existingTrigger", existingTrigger) // <<< Put the parcelable trigger in arguments >>>
                Timber.d("CalendarTriggerDialog: Existing trigger added to arguments.")
                }
            dialog.arguments = args
            return dialog
        }
    }
}
