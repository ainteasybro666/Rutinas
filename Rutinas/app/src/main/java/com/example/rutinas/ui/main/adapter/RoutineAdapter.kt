// RoutineAdapter.kt
package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.databinding.ItemRoutineBinding
import com.example.rutinas.domain.Routine
import timber.log.Timber

class RoutineAdapter(
    private val onSwitchChanged: (Routine, Boolean) -> Unit, // Callback para el cambio del switch
    private val onRoutineClicked: (Routine) -> Unit // Callback para el clic en el item
) : ListAdapter<Routine, RoutineAdapter.RoutineViewHolder>(RoutineDiffCallback()) {

    inner class RoutineViewHolder(
        private val binding: ItemRoutineBinding,
        private val onSwitchChanged: (Routine, Boolean) -> Unit,
        private val onRoutineClicked: (Routine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Configurar listener para el clic en el item completo de la rutina
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val routine = getItem(position)
                    Timber.d("Routine clicked in adapter: $routine")
                    onRoutineClicked(routine) // Llamar al callback onRoutineClicked con la Routine
                }
            }
        }

        fun bind(routine: Routine) {
            binding.tvRoutineName.text = routine.name // Este parece estar correcto en tu código
            binding.switchEnable.isChecked = routine.isEnabled

            binding.switchEnable.setOnCheckedChangeListener(null)
            binding.switchEnable.isChecked = routine.isEnabled
            binding.switchEnable.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged(routine, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineViewHolder(binding, onSwitchChanged, onRoutineClicked)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        val routine = getItem(position)
        holder.bind(routine)
    }
}

class RoutineDiffCallback : DiffUtil.ItemCallback<Routine>() {
    override fun areItemsTheSame(oldItem: Routine, newItem: Routine): Boolean {
        // Usar el ID (clave primaria) para verificar si son el mismo elemento
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Routine, newItem: Routine): Boolean {
        // Comprobar si el contenido es el mismo (comparar todas las propiedades relevantes)
        return oldItem == newItem
    }
}
