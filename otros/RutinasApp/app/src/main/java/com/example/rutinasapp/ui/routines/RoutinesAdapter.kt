package com.example.rutinasapp.ui.routines

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinasapp.data.RoutineEntity
import com.example.rutinasapp.databinding.ItemRoutineBinding

class RoutinesAdapter(
    private var routines: List<RoutineEntity>,
    private val onItemClick: (RoutineEntity) -> Unit,
    private val onSwitchChanged: (RoutineEntity, Boolean) -> Unit
) : RecyclerView.Adapter<RoutinesAdapter.RoutineViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(routines[position])
    }

    override fun getItemCount(): Int = routines.size

    inner class RoutineViewHolder(private val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(routine: RoutineEntity) {
            binding.tvRoutineName.text = routine.name
            binding.switchActive.isChecked = routine.isActive

            binding.root.setOnClickListener {
                onItemClick(routine)
            }

            binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged(routine, isChecked)
            }
        }
    }

    fun updateData(newRoutines: List<RoutineEntity>) {
        routines = newRoutines
        notifyDataSetChanged()
    }
}