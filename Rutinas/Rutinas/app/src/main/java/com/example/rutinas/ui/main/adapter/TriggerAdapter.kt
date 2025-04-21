package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.ItemTriggerBinding

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
                when (trigger.type) {
                    "TIME" -> {
                        val hour = trigger.hour
                        val minute = trigger.minute
                        val frequency = trigger.frequency
                        val daysOfWeek = trigger.daysOfWeek
                        val dayOfMonth = trigger.dayOfMonth

                        val timeText = String.format("%02d:%02d", hour, minute)

                        tvTriggerType.text = "Tiempo"
                        tvTriggerDetails.text = when (frequency) {
                            "daily" -> "Diario a las $timeText"
                            "weekly" -> "Semanalmente a las $timeText los días ${formatSelectedDays(daysOfWeek)}"
                            "monthly" -> "Mensualmente el día $dayOfMonth a las $timeText"
                            else -> "A las $timeText" // Fallback
                        }
                    }
                    "CALENDAR" -> {
                        tvTriggerType.text = "Calendario"
                        // Configurar detalles del calendario (aún no implementado)
                        tvTriggerDetails.text = "Detalles del calendario"
                    }
                    "LOCATION" -> {
                        tvTriggerType.text = "Ubicación"
                        // Configurar detalles de ubicación (aún no implementado)
                        tvTriggerDetails.text = "Detalles de ubicación"
                    }
                    else -> {
                        tvTriggerType.text = "Desconocido"
                        tvTriggerDetails.text = "Tipo de trigger desconocido"
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
                    1 -> "L"
                    2 -> "M"
                    3 -> "X"
                    4 -> "J"
                    5 -> "V"
                    6 -> "S"
                    7 -> "D"
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
