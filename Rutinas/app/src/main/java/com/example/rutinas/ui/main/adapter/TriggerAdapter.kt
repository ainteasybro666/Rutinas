package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.data.model.FrequencyType // Asegúrate de que esta importación sea correcta
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.ItemTriggerBinding
import com.example.rutinas.ui.edit.dialogs.TriggerTypeDialog // Asegúrate de que esta importación sea correcta
import timber.log.Timber

class TriggerAdapter(
    private val onTriggerDeleted: (Trigger) -> Unit,
    private val onTriggerClicked: (Trigger) -> Unit
) : ListAdapter<Trigger, TriggerAdapter.TriggerViewHolder>(TriggerDiffCallback()) {

    inner class TriggerViewHolder(
        private val binding: ItemTriggerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trigger: Trigger) {
            Timber.d("TriggerViewHolder: Binding trigger with UUID: ${trigger.uuid}, Type: ${trigger.triggerType}") // Log binding call
            // <<< Inspeccionar el mapa de datos crudo justo al inicio del bind
            Timber.d("TriggerViewHolder: Raw data for trigger UUID ${trigger.uuid}: ${trigger.data.data}")

            with(binding) {
                when (trigger.triggerType) {
                    TriggerTypeDialog.TriggerType.TIME -> {
                        Timber.d("TriggerViewHolder: Binding TIME trigger (UUID: ${trigger.uuid})")
                        // <<< USAR LOS NUEVOS MÉTODOS GETTER SEGUROS >>>
                        val hour = trigger.getInt("hour")
                        val minute = trigger.getInt("minute")
                        val frequency = trigger.getFrequencyType("frequency") // Usar el nuevo getter
                        val daysOfWeek = trigger.getDaysOfWeek("daysOfWeek") // Usar el nuevo getter
                        val dayOfMonth = trigger.getInt("dayOfMonth")

                        // <<< Los logs después del casting ahora mostrarán Int, FrequencyType, List<Int> >>>
                        Timber.d("TriggerViewHolder: TIME trigger data AFTER using safe getters (UUID: ${trigger.uuid}):")
                        Timber.d("  hour: $hour (Type: ${hour?.javaClass?.name})")
                        Timber.d("  minute: $minute (Type: ${minute?.javaClass?.name})")
                        Timber.d("  frequency: $frequency (Type: ${frequency?.javaClass?.name})")
                        Timber.d("  daysOfWeek: $daysOfWeek (Type: ${daysOfWeek?.javaClass?.name})") // Debería ser List<Int>
                        Timber.d("  dayOfMonth: $dayOfMonth (Type: ${dayOfMonth?.javaClass?.name})")
                        // <<< FIN DE LOS LOGS DETALLADOS DESPUÉS DEL CAST EN EL ADAPTER >>>


                        val timeText =
                            if (hour != null && minute != null) String.format("%02d:%02d", hour, minute) else "Hora no especificada"

                        tvTriggerType.text = "Tiempo"
                        // <<< Log antes de decidir el texto de detalles basado en la frecuencia
                        Timber.d("TriggerViewHolder: Deciding details text for UUID ${trigger.uuid}. Frequency is: $frequency")

                        tvTriggerDetails.text = when (frequency) {
                            FrequencyType.ONCE -> "A las $timeText"
                            FrequencyType.WEEKLY -> "Semanalmente a las $timeText los días ${formatSelectedDays(daysOfWeek)}"
                            FrequencyType.MONTHLY -> "Mensualmente el día $dayOfMonth a las $timeText"
                            else -> {
                                Timber.w("TriggerViewHolder: Unexpected or null frequency ($frequency) for TIME trigger UUID ${trigger.uuid}. Displaying fallback.")
                                "A las $timeText" // Fallback si frequency es null o inesperado
                            }
                        }
                        Timber.d("TriggerViewHolder: Details text set to: ${tvTriggerDetails.text} for UUID ${trigger.uuid}")
                    }

                    TriggerTypeDialog.TriggerType.CALENDAR -> {
                        Timber.d("TriggerViewHolder: Binding CALENDAR trigger (UUID: ${trigger.uuid})")
                        // <<< USAR LOS NUEVOS MÉTODOS GETTER SEGUROS >>>
                        val day = trigger.getInt("day") // Usar getInt
                        val month = trigger.getInt("month") // Usar getInt
                        val year = trigger.getInt("year") // Usar getInt
                        val calendarTime = trigger.getLocalDateTime("calendarTime") // Usar el getter para LocalDateTime si lo creaste

                        // <<< Logs específicos para los valores del trigger de calendario después del cast >>>
                        Timber.d("TriggerViewHolder: CALENDAR trigger data AFTER using safe getters (UUID: ${trigger.uuid}):")
                        Timber.d("  day: $day (Type: ${day?.javaClass?.name})")
                        Timber.d("  month: $month (Type: ${month?.javaClass?.name})")
                        Timber.d("  year: $year (Type: ${year?.javaClass?.name})")
                        if (calendarTime != null) Timber.d("  calendarTime: $calendarTime (Type: ${calendarTime?.javaClass?.name})")
                        // <<< FIN DE LOS LOGS >>>


                        tvTriggerType.text = "Calendario"
                        tvTriggerDetails.text = if (day != null && month != null && year != null) {
                            // Add 1 to month for display as months are often 0-indexed in code (0=Jan, 11=Dec)
                            val displayMonth = month + 1
                            Timber.d("TriggerViewHolder: Formatting Calendar date for UUID ${trigger.uuid}: Day=$day, Stored Month=$month, Display Month=$displayMonth, Year=$year")
                            String.format("El %02d/%02d/%d", day, displayMonth, year)
                        } else {
                            Timber.w("TriggerViewHolder: Incomplete Calendar data for UUID ${trigger.uuid}. Displaying fallback.")
                            "Fecha no especificada"
                        }
                        Timber.d("TriggerViewHolder: Details text set to: ${tvTriggerDetails.text} for UUID ${trigger.uuid}")
                    }

                    TriggerTypeDialog.TriggerType.LOCATION -> {
                        Timber.d("TriggerViewHolder: Binding LOCATION trigger (UUID: ${trigger.uuid})")
                        // <<< USAR LOS NUEVOS MÉTODOS GETTER SEGUROS >>>
                        val latitude = trigger.getDouble("latitude") // Usar getDouble
                        val longitude = trigger.getDouble("longitude") // Usar getDouble
                        val radius = trigger.getDouble("radius") // Usar getDouble
                        val locationName = trigger.getString("locationName") // Usar getString
                        val enterExit = trigger.getString("enterExit") // Usar getString

                        // <<< Logs específicos para los valores del trigger de ubicación después del cast >>>
                        Timber.d("TriggerViewHolder: LOCATION trigger data AFTER using safe getters (UUID: ${trigger.uuid}):")
                        Timber.d("  latitude: $latitude (Type: ${latitude?.javaClass?.name})")
                        Timber.d("  longitude: $longitude (Type: ${longitude?.javaClass?.name})")
                        Timber.d("  radius: $radius (Type: ${radius?.javaClass?.name})")
                        Timber.d("  locationName: $locationName (Type: ${locationName?.javaClass?.name})")
                        Timber.d("  enterExit: $enterExit (Type: ${enterExit?.javaClass?.name})")
                        // <<< FIN DE LOS LOGS >>>


                        tvTriggerType.text = "Ubicación"
                        tvTriggerDetails.text = if (latitude != null && longitude != null) { // Check for required fields
                            String.format("Lat: %.4f, Long: %.4f", latitude, longitude)
                        } else {
                            Timber.w("TriggerViewHolder: Incomplete LOCATION data for UUID ${trigger.uuid}. Displaying fallback.")
                            "Ubicación no especificada"
                        }
                        Timber.d("TriggerViewHolder: Details text set to: ${tvTriggerDetails.text} for UUID ${trigger.uuid}")
                    }

                    else -> {
                        Timber.d("TriggerViewHolder: Binding UNKNOWN trigger (UUID: ${trigger.uuid}, Type: ${trigger.triggerType})")
                        tvTriggerType.text = "Desconocido"
                        tvTriggerDetails.text = "Tipo de trigger desconocido"
                        Timber.w("TriggerViewHolder: Unknown trigger type encountered: ${trigger.triggerType} for UUID ${trigger.uuid}")
                    }
                }

                // Configurar botón de eliminar
                Timber.d("TriggerViewHolder: Setting up delete button for UUID ${trigger.uuid}")
                btnDelete.setOnClickListener {
                    Timber.d("TriggerViewHolder: Delete button clicked for UUID ${trigger.uuid}")
                    onTriggerDeleted(trigger)
                }
                // Configurar click en el item si tienes (no veo listener en tu código, pero es común)
                itemView.setOnClickListener {
                    Timber.d("TriggerViewHolder: Item clicked for UUID ${trigger.uuid}")
                    onTriggerClicked(trigger)
                }
            }
            Timber.d("TriggerViewHolder: Finished binding for UUID ${trigger.uuid}")
        }

        private fun formatSelectedDays(days: List<Int>?): String {
            Timber.d("TriggerViewHolder: Formatting daysOfWeek: $days")
            return days?.map { day ->
                when (day) {
                    0 -> "D"
                    1 -> "L"
                    2 -> "M"
                    3 -> "X"
                    4 -> "J"
                    5 -> "V"
                    6 -> "S"
                    else -> {
                        Timber.w("TriggerViewHolder: Unexpected day index in formatSelectedDays: $day")
                        "?"
                    }
                }
            }?.joinToString(", ") ?: ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerViewHolder {
        Timber.d("TriggerAdapter: onCreateViewHolder called")
        val binding = ItemTriggerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TriggerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TriggerViewHolder, position: Int) {
        Timber.d("TriggerAdapter: onBindViewHolder called for position $position")
        holder.bind(getItem(position))
        Timber.d("TriggerAdapter: onBindViewHolder finished for position $position")
    }

    class TriggerDiffCallback : DiffUtil.ItemCallback<Trigger>() {
        override fun areItemsTheSame(oldItem: Trigger, newItem: Trigger): Boolean {
            val areSame = oldItem.id == newItem.id
            Timber.d("TriggerDiffCallback: areItemsTheSame for ${oldItem.uuid} vs ${newItem.uuid}. Result: $areSame")
            return areSame
        }
        override fun areContentsTheSame(oldItem: Trigger, newItem: Trigger): Boolean {
            // Logging the full comparison can be noisy, but useful if areItemsTheSame is true but contents differ
            val areSame = oldItem == newItem
            Timber.d("TriggerDiffCallback: areContentsTheSame for ${oldItem.uuid} vs ${newItem.uuid}. Result: $areSame")
            return areSame
        }
    }
}
