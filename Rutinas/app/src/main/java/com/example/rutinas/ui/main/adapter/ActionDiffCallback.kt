package com.example.rutinas.ui.main.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.rutinas.data.model.Action
import timber.log.Timber // Asegúrate de tener la dependencia de Timber y la inicialización en tu Application class

class ActionDiffCallback : DiffUtil.ItemCallback<Action>() {

    override fun areItemsTheSame(oldItem: Action, newItem: Action): Boolean {
        // Items are the same if they have the same unique identifier (UUID)
        Timber.d("DiffCallback: areItemsTheSame called for UUIDs: ${oldItem.uuid} == ${newItem.uuid}")
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: Action, newItem: Action): Boolean {
        // Contents are the same if all relevant properties are equal
        Timber.d("DiffCallback: areContentsTheSame called for UUID: ${oldItem.uuid}. Old: $oldItem, New: $newItem")
        // IMPORTANT: DataWrapper MUST have a correct equals() implementation for this to work
        return oldItem.actionType == newItem.actionType &&
                oldItem.routineId == newItem.routineId &&
                oldItem.executionOrder == newItem.executionOrder &&
                oldItem.pauseDuration == newItem.pauseDuration &&
                oldItem.data == newItem.data
    }

    // Optional: You can implement this if you want to do partial updates
    // override fun getChangePayload(oldItem: Action, newItem: Action): Any? {
    //     // Compare specific fields to create a payload for partial updates
    //     return super.getChangePayload(oldItem, newItem)
    // }
}
