package com.example.rutinas.ui.main.adapter

import android.annotation.SuppressLint
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.databinding.ItemActionBinding
import com.example.rutinas.ui.edit.RoutineEditViewModel // Asegúrate de importar el ViewModel
import com.example.rutinas.ui.main.adapter.ActionDiffCallback
import timber.log.Timber
import java.util.Collections

// Define la interfaz ActionTouchHelperCallback
interface ActionTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean
    fun onItemDismiss(position: Int)
}

// ActionAdapter ahora recibe el ViewModel y callbacks
class ActionAdapter(
    private val viewModel: RoutineEditViewModel, // Si lo sigues necesitando aquí
    private val onActionDeleted: (Action) -> Unit,
    private val onActionClicked: (Action) -> Unit
) : ListAdapter<Action, ActionAdapter.ActionViewHolder>(ActionDiffCallback()), // Pasa la instancia de tu DiffCallback aquí
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
            Timber.d("ActionViewHolder: onBindViewHolder called for position: $adapterPosition, UUID: ${action.uuid}") // NEW Log
            Timber.d("ActionViewHolder: Binding action with UUID: ${action.uuid}, Type: ${action.actionType}, Order: ${action.executionOrder}") // Log binding call

            binding.txtActionTitle.text = action.actionType.name // O el nombre que prefieras mostrar
            Timber.d("ActionViewHolder: Setting title to: ${action.actionType.name}") // Log title
            binding.txtActionDescription.text = getActionDescription(action) // Mostrar detalles de la acción
            Timber.d("ActionViewHolder: Setting description to: ${binding.txtActionDescription.text}") // Log description

            // Optional: Log visibility of key elements
            Timber.d("ActionViewHolder: txtActionTitle visibility: ${binding.txtActionTitle.visibility}")
            Timber.d("ActionViewHolder: txtActionDescription visibility: ${binding.txtActionDescription.visibility}")
            Timber.d("ActionViewHolder: Item root view visibility: ${itemView.visibility}")

            binding.btnDeleteAction.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val actionToDelete = getItem(position)
                    Timber.d("Delete button clicked for action: $actionToDelete")
                    onActionDeleted(actionToDelete) // Llamar al callback onActionDeleted
                }
            }

            // Configurar el handle para el drag and drop
            binding.btnDragHandle.setOnTouchListener { view, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    // Iniciar el drag and drop
                    // Esto requiere acceso al ItemTouchHelper, que se puede obtener del RecyclerView
                    // O puedes manejarlo dentro de attachTouchHelper o en el Fragment
                    // Por ahora, mantenemos la implementación en onItemMove/onItemDismiss
                }
                false
            }
        }

        // Helper function to generate action description
        private fun getActionDescription(action: Action): String {
            Timber.d("getActionDescription: Generating description for action type: ${action.actionType}") // Log description generation
            return when (action.actionType) {
                ActionType.ALARM -> {   //TODO: Añadir labels a las acciones de alarma
                    val hour = action.data?.data?.get("hour") as? Int ?: 0
                    val minute = action.data?.data?.get("minute") as? Int ?: 0
                    val label = action.data?.data?.get("label") as? String
                    val time = String.format("%02d:%02d", hour, minute)
                    if (!label.isNullOrBlank()) {
                        "$label - $time"
                    } else {
                        "Programada para las $time"
                    }
                }
                ActionType.ANNOUNCEMENT -> {
                    val message = action.data?.data?.get("message") as? String ?: ""
                    val volume = action.data?.data?.get("volume") as? Int ?: 50
                    val truncatedMessage = if (message.length > 20) "${message.substring(0, 20)}..." else message
                    val volumeLevel = when {
                        volume <= 33 -> "bajo"
                        volume <= 66 -> "medio"
                        else -> "alto"
                    }
                    "$truncatedMessage - Vol $volumeLevel"
                }
                ActionType.BRIGHTNESS -> {
                    val value = action.data?.data?.get("value") as? Int ?: 100
                    val automatic = action.data?.data?.get("automatic") as? Boolean ?: false
                    if (automatic) {
                        "Brillo automático"
                    } else {
                        val brightnessLevel = when {
                            value <= 20 -> "muy bajo"
                            value <= 40 -> "bajo"
                            value <= 60 -> "medio"
                            value <= 80 -> "alto"
                            else -> "muy alto"
                        }
                        "Brillo: $brightnessLevel"
                    }
                } // Add other action types as needed
                ActionType.TIME -> {
                    // Assuming no specific display data needed for TIME action in the list
                    "Esperar"
                }
                ActionType.PAUSE -> {
                    val duration = action.data?.data?.get("duration") as? Long ?: 0L
                    "Pausa de ${duration / 1000} segundos" // Example: assuming duration is in milliseconds
                }
                // Add other action types as needed
                else -> action.actionType.name // Default to just the action type name
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
        // Dado que el ViewModel emite una nueva lista, submitList en el Fragmento manejará la actualización
        // notifyDataSetChanged() // Remove or comment this out

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

//class ActionDiffCallback : DiffUtil.ItemCallback<Action>() {
//    override fun areItemsTheSame(oldItem: Action, newItem: Action): Boolean {
//        // Usar UUID para verificar si son el mismo elemento
//        return oldItem.uuid == newItem.uuid
//    }

    fun areContentsTheSame(oldItem: Action, newItem: Action): Boolean {
        // Comprobar si el contenido es el mismo (comparar todas las propiedades relevantes)
        // Asegúrate de que la clase Action sea un data class o implemente equals/hashCode correctamente
        return oldItem == newItem
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
