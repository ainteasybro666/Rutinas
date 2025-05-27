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
import com.example.rutinas.data.model.FrequencyType
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.DialogTimeTriggerConfigBinding
import timber.log.Timber
import java.util.Calendar

class TimeTriggerConfigDialog : DialogFragment() {
    interface TimeTriggerConfigListener {
        fun onTimeTriggerConfigured(trigger: Trigger)
    }

    private var _binding: DialogTimeTriggerConfigBinding? = null
    private val binding get() = _binding!!
    private var timeTriggerConfigListener: TimeTriggerConfigListener? = null

    private var routineId: Long = 0
    private var existingTrigger: Trigger? = null // <<< Campo para guardar el trigger existente >>>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("TimeTriggerConfigDialog: onCreateDialog() llamado")
        _binding = DialogTimeTriggerConfigBinding.inflate(LayoutInflater.from(context))

        // <<< Recuperar el trigger existente de los argumentos si existe >>>
        existingTrigger = arguments?.getParcelable("existingTrigger") // Usa getParcelable
        routineId = arguments?.getLong("routineId") ?: 0 // Ya tenías esto

        setupUI() // Configura listeners y estado inicial por defecto

        // <<< AÑADIMOS LÓGICA PARA POBLAR LA UI SI ESTAMOS EDITANDO >>>
        existingTrigger?.let { trigger ->
            Timber.d("TimeTriggerConfigDialog: Editando trigger existente (UUID: ${trigger.uuid}). Poblanco UI...")
            populateUI(trigger) // Llama a una nueva función para poblar la UI
        } ?: run {
            Timber.d("TimeTriggerConfigDialog: Creando nuevo trigger. UI con valores por defecto.")
            // Si no hay trigger existente, la UI ya está configurada con valores por defecto por setupUI()
        }


        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("Aceptar") { _, _ -> configureTimeTrigger() }
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .create()
    }

    private fun setupUI() {
        Timber.d("TimeTriggerConfigDialog: setupUI() llamado. Configuranco listeners y valores por defecto.")
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
                    Timber.d("TimeTriggerConfigDialog: Frecuencia cambiada a Once. Campos de día ocultos.")
                }
                rbWeekly.id -> {
                    daysOfWeekGroup.visibility = View.VISIBLE
                    dayOfMonth.visibility = View.GONE
                    Timber.d("TimeTriggerConfigDialog: Frecuencia cambiada a Semanalmente. Campos de días visibles.")
                }
                rbMonthly.id -> {
                    daysOfWeekGroup.visibility = View.GONE
                    dayOfMonth.visibility = View.VISIBLE
                    dayOfMonth.minValue = 1
                    dayOfMonth.maxValue = 31
                    dayOfMonth.value = 1 // Establecer un valor por defecto al seleccionar Mensualmente
                    Timber.d("TimeTriggerConfigDialog: Frecuencia cambiada a Mensualmente. Campo de día del mes visible.")
                }
            }
        }

        // Establecer un valor por defecto al inicio (para la creación)
        // Si no hay trigger existente, establecemos ONCE y la hora actual
        if (existingTrigger == null) {
            rbOnce.isChecked = true // Set default checked radio button
            Timber.d("TimeTriggerConfigDialog: No existing trigger, setting default frequency to Once.")

            // Establecer la hora actual como valor por defecto si no hay trigger existente
            val calendar = Calendar.getInstance()
            binding.timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
            binding.timePicker.minute = calendar.get(Calendar.MINUTE)
            Timber.d("TimeTriggerConfigDialog: Setting default time to current time.")
        }


    }

    // <<< NUEVA FUNCIÓN PARA POBLAR LA UI DESDE UN TRIGGER EXISTENTE >>>
    private fun populateUI(trigger: Trigger) {
        Timber.d("TimeTriggerConfigDialog: populateUI() llamado para trigger UUID: ${trigger.uuid}")
        val hour = trigger.getInt("hour")
        val minute = trigger.getInt("minute")
        val frequency = trigger.getFrequencyType("frequency")
        val daysOfWeek = trigger.getDaysOfWeek("daysOfWeek")
        val dayOfMonth = trigger.getInt("dayOfMonth")

        // Poblar TimePicker
        if (hour != null && minute != null) {
            binding.timePicker.hour = hour
            binding.timePicker.minute = minute
            Timber.d("TimeTriggerConfigDialog: TimePicker set to $hour:$minute from trigger.")
        } else {
            Timber.w("TimeTriggerConfigDialog: Hour or minute is null in trigger data. TimePicker not set.")
        }

        // Poblar RadioGroup de frecuencia y mostrar/ocultar campos relevantes
        when (frequency) {
            FrequencyType.ONCE -> binding.rbOnce.isChecked = true
            FrequencyType.WEEKLY -> binding.rbWeekly.isChecked = true
            FrequencyType.MONTHLY -> binding.rbMonthly.isChecked = true
            else -> {
                binding.rbOnce.isChecked = true // Si la frecuencia es desconocida, por defecto a ONCE
                Timber.w("TimeTriggerConfigDialog: Unknown frequency type in trigger: $frequency. Defaulting to Once.")
            }
        }
        // El setOnCheckedChangeListener de rgFrequency se encargará de mostrar/ocultar daysOfWeekGroup y dayOfMonth
        // inmediatamente después de establecer el botón de radio seleccionado.

        // Poblar Checkboxes de días (solo si la frecuencia es SEMANAL)
        // Asegurarse de que daysOfWeekGroup esté visible antes de poblar si frequency es WEEKLY
        if (frequency == FrequencyType.WEEKLY) {
            binding.daysOfWeekGroup.visibility = View.VISIBLE // Asegurarse de que esté visible
            // Desmarcar todos primero
            binding.cbSunday.isChecked = false
            binding.cbMonday.isChecked = false
            binding.cbTuesday.isChecked = false
            binding.cbWednesday.isChecked = false
            binding.cbThursday.isChecked = false
            binding.cbFriday.isChecked = false
            binding.cbSaturday.isChecked = false

            // Marcar los días que están en la lista del trigger
            daysOfWeek?.forEach { day ->
                // Los valores en tu layout para los checkboxes son 0-6 (D-S)
                // Y en tu trigger (getDaysOfWeek) parecen ser 0-6 o 1-7 (depende de cómo los guardaste).
                // Asegúrate de que este mapeo sea correcto. Asumiendo que 0=Domingo, 1=Lunes, etc.
                when (day) {
                    0 -> binding.cbSunday.isChecked = true
                    1 -> binding.cbMonday.isChecked = true
                    2 -> binding.cbTuesday.isChecked = true
                    3 -> binding.cbWednesday.isChecked = true
                    4 -> binding.cbThursday.isChecked = true
                    5 -> binding.cbFriday.isChecked = true
                    6 -> binding.cbSaturday.isChecked = true
                    else -> Timber.w("TimeTriggerConfigDialog: Unexpected day of week value in trigger data: $day")
                }
            }
            Timber.d("TimeTriggerConfigDialog: DaysOfWeek checkboxes populated from trigger data.")

        } else {
            binding.daysOfWeekGroup.visibility = View.GONE // Asegurarse de que esté oculto si no es SEMANAL
        }


        // Poblar NumberPicker de día del mes (solo si la frecuencia es MENSUAL)
        if (frequency == FrequencyType.MONTHLY && dayOfMonth != null) {
            binding.dayOfMonth.visibility = View.VISIBLE // Asegurarse de que esté visible
            binding.dayOfMonth.minValue = 1
            binding.dayOfMonth.maxValue = 31
            binding.dayOfMonth.value = dayOfMonth
            Timber.d("TimeTriggerConfigDialog: DayOfMonth NumberPicker set to $dayOfMonth from trigger.")
        } else {
            binding.dayOfMonth.visibility = View.GONE // Asegurarse de que esté oculto si no es MENSUAL o el valor es nulo
        }

        Timber.d("TimeTriggerConfigDialog: UI populated with existing trigger data.")
    }


    private fun configureTimeTrigger() {
        Timber.d("TimeTriggerConfigDialog: configureTimeTrigger() llamado")
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val frequency = when (binding.rgFrequency.checkedRadioButtonId) {
            binding.rbWeekly.id -> FrequencyType.WEEKLY
            binding.rbMonthly.id -> FrequencyType.MONTHLY
            else -> FrequencyType.ONCE // Default to "once" if no other option is selected
        }
        val daysOfWeek = if (frequency == FrequencyType.WEEKLY) {
            // Aquí mapeas de los Checkboxes a la lista de Int para guardar.
            // Asegúrate de que el mapeo coincida con cómo esperas leerlo en getDaysOfWeek.
            // Si en getDaysOfWeek esperas 0-6 (Domingo-Sábado), el mapeo aquí debe ser:
            listOf(
                binding.cbSunday.isChecked, // 0: Domingo
                binding.cbMonday.isChecked, // 1: Lunes
                binding.cbTuesday.isChecked, // 2: Martes
                binding.cbWednesday.isChecked, // 3: Miércoles
                binding.cbThursday.isChecked, // 4: Jueves
                binding.cbFriday.isChecked, // 5: Viernes
                binding.cbSaturday.isChecked // 6: Sábado
            ).mapIndexedNotNull { index, isChecked -> if (isChecked) index else null }
        } else {
            emptyList()
        }
        val dayOfMonth = if (frequency == FrequencyType.MONTHLY) {
            binding.dayOfMonth.value
        } else {
            1 // Default to 1 if not monthly
        }

        // <<< CREAR/ACTUALIZAR el objeto Trigger >>>
        val triggerToSave = existingTrigger?.copy( // Si existingTrigger no es null, crea una copia
            routineId = routineId, // Mantener el routineId
            data = DataWrapper( // Actualizar el data con los nuevos valores
                mapOf(
                    "hour" to hour,
                    "minute" to minute,
                    "frequency" to frequency,
                    "daysOfWeek" to daysOfWeek,
                    "dayOfMonth" to dayOfMonth
                )
            )
        ) ?: Trigger( // Si existingTrigger es null, crea un nuevo Trigger
            routineId = routineId,
            triggerType = TriggerTypeDialog.TriggerType.TIME, // Asegúrate de que TriggerTypeDialog esté accesible
            data = DataWrapper(
                mapOf(
                    "hour" to hour,
                    "minute" to minute,
                    "frequency" to frequency,
                    "daysOfWeek" to daysOfWeek,
                    "dayOfMonth" to dayOfMonth
                )
            )
            // El UUID se generará por defecto al crear un nuevo Trigger
        )
        Timber.d("TimeTriggerConfigDialog: Configurado trigger (UUID: ${triggerToSave.uuid}, RoutineId: ${triggerToSave.routineId}). Llamando listener.")

        timeTriggerConfigListener?.onTimeTriggerConfigured(triggerToSave)
    }

    fun setTimeTriggerConfigListener(listener: TimeTriggerConfigListener) {
        this.timeTriggerConfigListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("TimeTriggerConfigDialog: onDestroyView() llamado.")
    }

    companion object {
        // <<< Modificar createInstance para aceptar un Trigger opcional >>>
        fun createInstance(routineId: Long, existingTrigger: Trigger? = null): TimeTriggerConfigDialog {
            Timber.d("TimeTriggerConfigDialog: createInstance() llamado. RoutineId: $routineId, ExistingTrigger: ${existingTrigger?.uuid ?: "null"}")
            val dialog = TimeTriggerConfigDialog()
            val args = Bundle()
            args.putLong("routineId", routineId)
            if (existingTrigger != null) {
                args.putParcelable("existingTrigger", existingTrigger) // Poner el trigger parcelable en los argumentos
                Timber.d("TimeTriggerConfigDialog: Existing trigger added to arguments.")
            }
            dialog.arguments = args
            return dialog
        }
    }
}