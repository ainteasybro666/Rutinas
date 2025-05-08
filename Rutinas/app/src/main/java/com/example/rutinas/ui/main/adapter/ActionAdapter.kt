package com.example.rutinas.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.ItemActionBinding
import com.example.rutinas.ui.edit.RoutineEditViewModel

class ActionAdapter(
    private val viewModel: RoutineEditViewModel,
    private val onActionDeleted: (Action) -> Unit,
    private val onActionClicked: (Action) -> Unit
) : ListAdapter<Action, ActionAdapter.ActionViewHolder>(ActionDiffCallback()) {

    private var touchHelper: ItemTouchHelper? = null

    inner class ActionViewHolder(private val binding: ItemActionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(action: Action) {
            with(binding) {
                txtActionTitle.text = action.title
                txtActionDescription.text = action.description
                btnDeleteAction.setOnClickListener { onActionDeleted(action) }
                root.setOnClickListener { onActionClicked(action) }
            }
        }
    }

    fun attachTouchHelper(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.adapterPosition
                moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        touchHelper = ItemTouchHelper(callback).apply {
            attachToRecyclerView(recyclerView)
        }
    }

    private fun moveItem(from: Int, to: Int) {
        val newList = currentList.toMutableList().apply {
            add(to, removeAt(from))
        }
        submitList(newList) // Update adapter's list
        // Notify ViewModel about the reordered list
        viewModel.onActionListReordered(newList.mapIndexed { index, action ->
            action.copy(executionOrder = index)
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val binding = ItemActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActionDiffCallback : DiffUtil.ItemCallback<Action>() {
        override fun areItemsTheSame(oldItem: Action, newItem: Action): Boolean = oldItem.uuid == newItem.uuid // Use UUID for stable item identification
        override fun areContentsTheSame(oldItem: Action, newItem: Action): Boolean = oldItem == newItem
    }
}
