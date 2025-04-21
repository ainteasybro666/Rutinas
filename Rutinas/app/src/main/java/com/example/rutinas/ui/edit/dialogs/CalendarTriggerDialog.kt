package com.example.rutinas.ui.edit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.DialogCalendarTriggerBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class CalendarTriggerDialog : DialogFragment() {

    interface CalendarTriggerListener {
        fun onCalendarTriggerConfigured(trigger: Trigger)
    }

    private var listener: CalendarTriggerListener? = null
    private lateinit var binding: DialogCalendarTriggerBinding

    fun setCalendarTriggerListener(listener: CalendarTriggerListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogCalendarTriggerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // El DatePicker suele inicializarse con la fecha actual.
        binding.btnConfirm.setOnClickListener {
            val day = binding.datePicker.dayOfMonth
            val month = binding.datePicker.month
            val year = binding.datePicker.year

            val configData = mapOf("day" to day, "month" to month, "year" to year)
            val trigger = Trigger(
                uuid = UUID.randomUUID().toString(),
                routineId = 0, // routineId se asigna al guardar la rutina
                triggerType = "CALENDAR",
                data = configData
            )
            listener?.onCalendarTriggerConfigured(trigger)
            dismiss()
        }
    }
}