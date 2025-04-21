package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.databinding.ItemRoutineBinding
import com.example.rutinas.domain.Routine

class RoutineAdapter(
    private val onSwitchChanged: (Routine, Boolean) -> Unit
) : ListAdapter<Routine, RoutineAdapter.ViewHolder>(RoutineDiffCallback()) {

    inner class ViewHolder(private val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(routine: Routine) {
            binding.apply {
                tvRoutineName.text = routine.name
                // Resetear el listener para evitar callbacks múltiples
                switchEnable.setOnCheckedChangeListener(null)
                switchEnable.isChecked = routine.isEnabled
                switchEnable.setOnCheckedChangeListener { _, isChecked ->
                    onSwitchChanged(routine, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Usamos un ArrayList para evitar problemas de mutabilidad.
    override fun submitList(list: List<Routine>?) {
        super.submitList(list?.let { ArrayList(it) })
    }
}

class RoutineDiffCallback : DiffUtil.ItemCallback<Routine>() {
    override fun areItemsTheSame(oldItem: Routine, newItem: Routine): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Routine, newItem: Routine): Boolean {
        return oldItem == newItem
    }
}
