package com.example.rutinas.ui.edit.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.databinding.DialogActionSelectionBinding
import timber.log.Timber
import java.util.UUID

class AddActionDialogFragment : DialogFragment() {
    private var _binding: DialogActionSelectionBinding? = null
    private val binding get() = _binding!!
    private var onActionSelectedListener: ((Action) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("AddActionDialogFragment: onCreateDialog() llamado")
        _binding = DialogActionSelectionBinding.inflate(layoutInflater)

        // Crear el diálogo directamente con el contenido de selección de acciones
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Acción")
            .setView(binding.root)
            .setNegativeButton("Cancelar") { _, _ ->
                Timber.d("AddActionDialogFragment: Botón Cancelar pulsado")
                dismiss()
            }
            .create()

        // Configurar las categorías directamente en este diálogo
        setupCategories()

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        return dialog
    }

    private fun setupCategories() {
        Timber.d("AddActionDialogFragment: setupCategories() llamado")
        try {
            // Verificar si el contenedor existe
            if (binding.categoriesContainer == null) {
                Timber.e("AddActionDialogFragment: El contenedor categoriesContainer es nulo")
                return
            }

            // Limpiar el contenedor
            binding.categoriesContainer.removeAllViews()

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
                Timber.d("AddActionDialogFragment: Inflando categoría: ${category.name}")
                val categoryView = inflater.inflate(R.layout.item_category, binding.categoriesContainer, false)
                val categoryTitle = categoryView.findViewById<TextView>(R.id.categoryTitle)
                val actionsContainer = categoryView.findViewById<LinearLayout>(R.id.actionsContainer)

                categoryTitle.text = category.name
                categoryTitle.setOnClickListener {
                    Timber.d("AddActionDialogFragment: Categoría pulsada: ${category.name}")
                    // Alternar visibilidad del contenedor de acciones
                    actionsContainer.visibility = if (actionsContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }

                // Inflar las acciones de esta categoría
                for (action in category.actions) {
                    Timber.d("AddActionDialogFragment: Inflando acción: ${action.name}")
                    val actionView = inflater.inflate(R.layout.list_item_action, actionsContainer, false)
                    val actionName = actionView.findViewById<TextView>(R.id.tvActionName)

                    actionName.text = action.name
                    actionName.setCompoundDrawablesWithIntrinsicBounds(action.iconResId, 0, 0, 0)

                    actionView.setOnClickListener {
                        Timber.d("AddActionDialogFragment: Acción seleccionada: ${action.name}")
                        val newAction = Action(
                            id = UUID.randomUUID().toString(),
                            routineId = 0,
                            type = action.type,
                            data = emptyMap(),
                            executionOrder = 0
                        )
                        onActionSelectedListener?.invoke(newAction)
                        dismiss()
                    }

                    actionsContainer.addView(actionView)
                }

                binding.categoriesContainer.addView(categoryView)
            }

            Timber.d("AddActionDialogFragment: Categorías configuradas correctamente")
        } catch (e: Exception) {
            Timber.e("AddActionDialogFragment: Error al configurar categorías - ${e.message}")
            e.printStackTrace()
        }
    }

    fun setOnActionSelectedListener(listener: (Action) -> Unit) {
        onActionSelectedListener = listener
    }

    override fun onDestroyView() {
        Timber.d("AddActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AddActionDialogFragment()
    }

    // Clases de datos para categorías y acciones
    data class Category(val name: String, val actions: List<ActionItem>)
    data class ActionItem(val type: String, val name: String, val iconResId: Int)
}