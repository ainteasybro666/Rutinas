package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.ItemTriggerBinding
import timber.log.Timber

class TriggerAdapter(
    private val triggers: List<Trigger>,
    private val onTriggerDeleted: (Trigger) -> Unit
) : RecyclerView.Adapter<TriggerAdapter.TriggerViewHolder>() {

    inner class TriggerViewHolder(
        private val binding: ItemTriggerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trigger: Trigger) {
            with(binding) {
                // Configurar el texto según el tipo de trigger
                when (trigger.triggerType) {
                    "TIME" -> {
                        val hour = trigger.data["hour"] as? Int
                        val minute = trigger.data["minute"] as? Int
                        val frequency = trigger.data["frequency"] as? String
                        val daysOfWeek = trigger.data["daysOfWeek"] as? List<Int>
                        val dayOfMonth = trigger.data["dayOfMonth"] as? Int

                        val timeText = if (hour != null && minute != null) String.format("%02d:%02d", hour, minute) else "Hora no especificada"

                        tvTriggerType.text = "Tiempo"
                        tvTriggerDetails.text = when (frequency) {
                            "daily" -> "Diario a las $timeText"
                            "weekly" -> "Semanalmente a las $timeText los días ${formatSelectedDays(daysOfWeek)}"
                            "monthly" -> "Mensualmente el día $dayOfMonth a las $timeText"
                            else -> "A las $timeText" // Fallback
                        }
                    }
                    "CALENDAR" -> {
                        val day = trigger.data["day"] as? Int
                        val month = trigger.data["month"] as? Int
                        val year = trigger.data["year"] as? Int

                        tvTriggerType.text = "Calendario"
                        tvTriggerDetails.text = if (day != null && month != null && year != null) {
                            String.format("El %02d/%02d/%d", day, month + 1, year) // month is 0-indexed
                        } else {
                            "Fecha no especificada"
                        }
                    }
                    "LOCATION" -> {
                        val latitude = trigger.data["latitude"] as? Double
                        val longitude = trigger.data["longitude"] as? Double

                        tvTriggerType.text = "Ubicación"
                        tvTriggerDetails.text = if (latitude != null && longitude != null) {
                            String.format("Lat: %.4f, Long: %.4f", latitude, longitude)
                        } else {
                            "Ubicación no especificada"
                        }
                    }
                    else -> {
                        tvTriggerType.text = "Desconocido"
                        tvTriggerDetails.text = "Tipo de trigger desconocido"
                        Timber.w("Tipo de trigger desconocido: ${trigger.triggerType}")
                    }
                }

                // Configurar botón de eliminar
                btnDelete.setOnClickListener {
                    onTriggerDeleted(trigger)
                }
            }
        }

        private fun formatSelectedDays(days: List<Int>?): String {
            return days?.map { day ->
                when (day) {
                    0 -> "D"
                    1 -> "L"
                    2 -> "M"
                    3 -> "X"
                    4 -> "J"
                    5 -> "V"
                    6 -> "S"
                    else -> "?"
                }
            }?.joinToString(", ") ?: ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerViewHolder {
        val binding = ItemTriggerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TriggerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TriggerViewHolder, position: Int) {
        holder.bind(triggers[position])
    }

    override fun getItemCount(): Int = triggers.size
}