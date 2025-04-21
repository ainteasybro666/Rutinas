package com.example.rutinasapp.ui.routines

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinasapp.data.ActionEntity
import com.example.rutinasapp.databinding.ItemActionBinding

class ActionsAdapter(
    private var actions: MutableList<ActionEntity>,
    private val onActionChanged: (List<ActionEntity>) -> Unit
) : RecyclerView.Adapter<ActionsAdapter.ActionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val binding = ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.bind(actions[position])
    }

    override fun getItemCount(): Int = actions.size

    fun updateData(newActions: List<ActionEntity>) {
        actions.clear()
        actions.addAll(newActions)
        notifyDataSetChanged()
    }

    inner class ActionViewHolder(private val binding: ItemActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(action: ActionEntity) {
            binding.tvActionDescription.text = action.description
            // Aquí puedes agregar más lógica para mostrar otros atributos de ActionEntity
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val movedItem = actions.removeAt(fromPosition)
        actions.add(if (toPosition > fromPosition) toPosition - 1 else toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
        onActionChanged(actions) // Notificar cambios
    }
}