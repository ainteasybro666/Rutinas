package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.ItemTriggerBinding
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
            with(binding) {
                // Configurar el texto según el tipo de trigger
                when (trigger.triggerType) {
                    "TIME" -> {
                        val hour = trigger.data.data["hour"] as? Int
                        val minute = trigger.data.data["minute"] as? Int
                        val frequency = trigger.data.data["frequency"] as? String
                        val daysOfWeek = trigger.data.data["daysOfWeek"] as? List<Int>
                        val dayOfMonth = trigger.data.data["dayOfMonth"] as? Int

                        val timeText =
                            if (hour != null && minute != null) String.format("%02d:%02d", hour, minute) else "Hora no especificada"

                        tvTriggerType.text = "Tiempo"
                        tvTriggerDetails.text = when (frequency) {
                            "daily" -> "Diario a las $timeText"
                            "weekly" -> "Semanalmente a las $timeText los días ${formatSelectedDays(daysOfWeek)}"
                            "monthly" -> "Mensualmente el día $dayOfMonth a las $timeText"
                            else -> "A las $timeText" // Fallback
                        }
                    }

                    "CALENDAR" -> {
                        val day = trigger.data.data["day"] as? Int
                        val month = trigger.data.data["month"] as? Int
                        val year = trigger.data.data["year"] as? Int

                        tvTriggerType.text = "Calendario"
                        tvTriggerDetails.text = if (day != null && month != null && year != null) {
                            String.format("El %02d/%02d/%d", day, month + 1, year) // month is 0-indexed
                        } else {
                            "Fecha no especificada"
                        }
                    }

                    "LOCATION" -> {
                        val latitude = trigger.data.data["latitude"] as? Double
                        val longitude = trigger.data.data["longitude"] as? Double

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
        holder.bind(getItem(position))
    }

    class TriggerDiffCallback : DiffUtil.ItemCallback<Trigger>() {
        override fun areItemsTheSame(oldItem: Trigger, newItem: Trigger): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Trigger, newItem: Trigger): Boolean = oldItem == newItem
    }
}
