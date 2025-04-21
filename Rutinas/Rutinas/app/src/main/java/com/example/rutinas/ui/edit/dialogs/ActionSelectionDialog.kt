package com.example.rutinas.ui.edit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.databinding.DialogActionSelectionBinding
import timber.log.Timber
import java.util.UUID

class ActionSelectionDialog : Fragment() {
    private var _binding: DialogActionSelectionBinding? = null
    private val binding get() = _binding!!
    private var onActionSelectedListener: ((Action) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("ActionSelectionDialog: onCreateView() llamado")
        _binding = DialogActionSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("ActionSelectionDialog: onViewCreated() llamado")
        setupCategories(binding.categoriesContainer)
    }

    fun setupCategories(container: LinearLayout) {
        Timber.d("ActionSelectionDialog: setupCategories() llamado")
        try {
            // Limpiar el contenedor
            container.removeAllViews()

            // Definir las categorías y acciones
            val categories = listOf(
                Category("Comunicación", listOf(
                    ActionItem(ActionType.ANNOUNCEMENT.name, "Anuncio", R.drawable.ic_announcement),
                    ActionItem(ActionType.NOTIFICATIONS.name, "Leer Notificaciones", R.drawable.ic_notifications)
                )),
                Category("Dispositivo", listOf(
                    ActionItem(ActionType.BRIGHTNESS.name, "Ajustar Brillo", R.drawable.ic_brightness),
                    ActionItem(ActionType.VOLUME.name, "Ajustar Volumen", R.drawable.ic_volume),
                    ActionItem(ActionType.SOUND_MODE.name, "Modo de Sonido", R.drawable.ic_sound_mode)
                )),
                Category("Utilidades", listOf(
                    ActionItem(ActionType.ALARM.name, "Alarma", R.drawable.ic_alarm),
                    ActionItem(ActionType.TIME.name, "Decir la Hora", R.drawable.ic_time)
                ))
            )

            // Inflar las categorías y acciones
            val inflater = LayoutInflater.from(requireContext())
            for (category in categories) {
                Timber.d("ActionSelectionDialog: Inflando categoría: ${category.name}")
                val categoryView = inflater.inflate(R.layout.item_category, container, false)
                val categoryTitle = categoryView.findViewById<TextView>(R.id.categoryTitle)
                val actionsContainer = categoryView.findViewById<LinearLayout>(R.id.actionsContainer)

                categoryTitle.text = category.name
                categoryTitle.setOnClickListener {
                    Timber.d("ActionSelectionDialog: Categoría pulsada: ${category.name}")
                    // Alternar visibilidad del contenedor de acciones
                    actionsContainer.visibility = if (actionsContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }

                // Inflar las acciones de esta categoría
                for (action in category.actions) {
                    Timber.d("ActionSelectionDialog: Inflando acción: ${action.name}")
                    val actionView = inflater.inflate(R.layout.list_item_action, actionsContainer, false)
                    val actionName = actionView.findViewById<TextView>(R.id.tvActionName)

                    actionName.text = action.name
                    actionName.setCompoundDrawablesWithIntrinsicBounds(action.iconResId, 0, 0, 0)

                    actionView.setOnClickListener {
                        Timber.d("ActionSelectionDialog: Acción seleccionada: ${action.name}")
                        val newAction = Action(
                            id = UUID.randomUUID().toString(),
                            routineId = 0,
                            type = action.type,
                            data = emptyMap(),
                            executionOrder = 0
                        )
                        onActionSelectedListener?.invoke(newAction)
                    }

                    actionsContainer.addView(actionView)
                }

                container.addView(categoryView)
            }

            Timber.d("ActionSelectionDialog: Categorías configuradas correctamente")
        } catch (e: Exception) {
            Timber.e("ActionSelectionDialog: Error al configurar categorías - ${e.message}")
            e.printStackTrace()
        }
    }

    fun setOnActionSelectedListener(listener: (Action) -> Unit) {
        onActionSelectedListener = listener
    }

    override fun onDestroyView() {
        Timber.d("ActionSelectionDialog: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ActionSelectionDialog {
            return ActionSelectionDialog()
        }
    }

    data class Category(val name: String, val actions: List<ActionItem>)
    data class ActionItem(val type: String, val name: String, val iconResId: Int)
}
