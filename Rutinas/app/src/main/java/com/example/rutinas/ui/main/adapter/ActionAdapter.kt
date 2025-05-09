package com.example.rutinas.ui.main.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.databinding.ItemActionBinding
import com.example.rutinas.ui.edit.RoutineEditViewModel // Importa el ViewModel
import timber.log.Timber
import java.util.Collections

// Define la interfaz ActionTouchHelperCallback
interface ActionTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
    fun onItemDismiss(position: Int)
}

// ActionAdapter ahora recibe el ViewModel y callbacks
class ActionAdapter(
    private val viewModel: RoutineEditViewModel, // Recibe el ViewModel
    private val onActionDeleted: (Action) -> Unit, // Callback para eliminar
    private val onActionClicked: (Action) -> Unit // Callback para hacer clic
) : ListAdapter<Action, ActionAdapter.ActionViewHolder>(ActionDiffCallback()),
    ActionTouchHelperAdapter { // Implementa la interfaz

    inner class ActionViewHolder(
        private val binding: ItemActionBinding,
        private val onActionDeleted: (Action) -> Unit,
        private val onActionClicked: (Action) -> Unit,
        private val viewModel: RoutineEditViewModel // Pasar el ViewModel al ViewHolder
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Configurar listener para el clic en el item
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val action = getItem(position)
                    Timber.d("Action clicked in adapter: $action")
                    onActionClicked(action) // Llamar al callback onActionClicked con la Action
                }
            }
        }

        fun bind(action: Action) {
            Timber.d("ActionViewHolder: Binding action with UUID: ${action.uuid}, Type: ${action.actionType}, Order: ${action.executionOrder}") // Log binding call
            binding.txtActionTitle.text = action.actionType.name // O el nombre que prefieras mostrar
            binding.txtActionDescription.text = action.actionType.name // Mostrar detalles de la acción si es necesario

            binding.btnDeleteAction.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val actionToDelete = getItem(position)
                    Timber.d("Delete button clicked for action: $actionToDelete")
                    onActionDeleted(actionToDelete) // Llamar al callback onActionDeleted
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val binding = ItemActionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActionViewHolder(binding, onActionDeleted, onActionClicked, viewModel) // Pasar ViewModel
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        val action = getItem(position)
        holder.bind(action)
    }

    // Implementación de ActionTouchHelperAdapter
    @SuppressLint("NotifyDataSetChanged")
    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Timber.d("onItemMove: Moving item from $fromPosition to $toPosition")
        // Obtener la lista mutable actual del ViewModel
        val currentList = viewModel.actions.value.toMutableList()

        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(currentList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(currentList, i, i - 1)
            }
        }

        // Actualizar el executionOrder después del movimiento
        currentList.forEachIndexed { index, action ->
            action.executionOrder = index
        }

        // Notificar al ViewModel del cambio en el orden
        viewModel.onActionListReordered(currentList) // Llamar a la función en el ViewModel

        // Notificar al adaptador del cambio visual
        // Usamos notifyDataSetChanged temporalmente para simplificar, considera un enfoque más granular si el rendimiento es crítico
        notifyDataSetChanged()

        return true
    }

    override fun onItemDismiss(position: Int) {
        Timber.d("onItemDismiss: Dismissing item at position $position")
        val actionToDelete = getItem(position)
        onActionDeleted(actionToDelete) // Llamar al callback onActionDeleted
    }

    // Helper function to attach the ItemTouchHelper
    fun attachTouchHelper(recyclerView: RecyclerView) {
        val callback = ActionTouchHelperCallback(this)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}

class ActionDiffCallback : DiffUtil.ItemCallback<Action>() {
    override fun areItemsTheSame(oldItem: Action, newItem: Action): Boolean {
        // Usar UUID para verificar si son el mismo elemento
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: Action, newItem: Action): Boolean {
        // Comprobar si el contenido es el mismo (comparar todas las propiedades relevantes)
        return oldItem == newItem
    }
}

// Implementación del ItemTouchHelper.Callback
class ActionTouchHelperCallback(private val adapter: ActionTouchHelperAdapter) :
    ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean = true
    override fun isItemViewSwipeEnabled(): Boolean = true

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.onItemDismiss(viewHolder.adapterPosition)
    }
}
