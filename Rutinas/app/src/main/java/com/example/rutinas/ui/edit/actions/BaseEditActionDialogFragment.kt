package com.example.rutinas.ui.edit.actions

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding // Importar ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.ui.edit.ActionDialogListener
import timber.log.Timber

//Eliminar @AndroidEntryPoint de aquí
// @AndroidEntryPoint
// Cambiar de vuelta a T : ViewBinding
abstract class BaseEditActionDialogFragment<T : ViewBinding>(protected val listener: ActionDialogListener) : DialogFragment() {

    // To avoid showing the error dialog multiple times.
    private var errorDialogShown = false

    // Declarar _binding como protected y inicialízalo en la subclase
    protected var _binding: T? = null
    protected val binding get() = _binding!!

    protected lateinit var actionToEdit: Action // Cambiar nombre para mayor claridad

    companion object {
        const val ARG_ACTION = "arg_action"
        fun newBundle(action: Action) = bundleOf(ARG_ACTION to action)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            actionToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getParcelable(ARG_ACTION, Action::class.java)
                    ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            } else {
                @Suppress("DEPRECATION")
                requireArguments().getParcelable(ARG_ACTION)
                    ?: throw IllegalArgumentException("Se requiere una instancia válida de Action en los argumentos usando la clave '$ARG_ACTION'.")
            }
            Timber.d("${this.javaClass.simpleName}: Acción recibida - tipo: ${actionToEdit.actionType}, datos: ${actionToEdit.data?.data}")
        } catch (e: Exception) {
            Timber.e("${this.javaClass.simpleName}: Error al obtener la acción - ${e.message}")
            e.printStackTrace()
            if (!errorDialogShown) {
                errorDialogShown = true
                showErrorDialog(e.message ?: "Error desconocido al cargar la acción")
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return try {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            // Inflar el binding en la subclase y pasarlo a setView
            _binding = inflateBinding(layoutInflater, null)

            // Usar safe call y Elvis operator para proporcionar una vista vacía por defecto si binding es null
            _binding?.let {
                dialogBuilder.setView(it.root)
            } ?: throw IllegalStateException("Binding no se pudo inflar.")


            // ** >>>>> INICIO: Configurar botones Guardar y Cancelar aquí <<<<< **
            dialogBuilder.setPositiveButton("Guardar") { _, _ ->
                Timber.d("${this.javaClass.simpleName}: Botón Guardar pulsado")
                onSaveActionClicked() // Llamar a un nuevo método que maneja el guardado y cierre
            }
            dialogBuilder.setNegativeButton("Cancelar") { _, _ ->
                Timber.d("${this.javaClass.simpleName}: Botón Cancelar pulsado")
                // El diálogo se cierra automáticamente al hacer clic en el botón negativo por defecto.
                // Puedes añadir lógica adicional aquí si es necesario antes de cerrar.
            }
            // ** >>>>> FIN: Configurar botones Guardar y Cancelar aquí <<<<< **


            val dialog = dialogBuilder.create()
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Asegurarse de que loadActionData se llama DESPUÉS de que el binding esté inflado
            // y antes de mostrar el diálogo si es posible, o en onActivityCreated.
            // Vamos a llamarlo en onActivityCreated para asegurarnos de que el contexto y la vista están listos.

            dialog
        } catch (e: Exception) {
            Timber.e("BaseEditActionDialogFragment: Error al crear el diálogo - ${e.message}")
            e.printStackTrace()
            showErrorDialog("Error al crear el diálogo de configuración: ${e.message}")
            AlertDialog.Builder(requireContext()).create()
        }
    }

    // Llamar a loadActionData aquí para asegurar que el binding esté listo
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("${this.javaClass.simpleName}: onActivityCreated, calling loadActionData")
        loadActionData(actionToEdit)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpiar el binding
        Timber.d("${this.javaClass.simpleName}: onDestroyView, binding cleaned up")
    }

    protected fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Aceptar") { _, _ ->
                dismiss()
            }
            .show()
        Timber.e("${this.javaClass.simpleName}: Showing error dialog: $message")
    }

    // Nuevo método para manejar el clic del botón Guardar
    private fun onSaveActionClicked() {
        try {
            val updatedAction = saveActionData() // Llama al método de la subclase para obtener los datos
            notifyActionUpdated(updatedAction) // Notifica al listener
            Timber.d("${this.javaClass.simpleName}: Acción actualizada y notificada. Cerrando diálogo.")
            dismiss() // Cerrar el diálogo
        } catch (e: Exception) {
            Timber.e("${this.javaClass.simpleName}: Error al guardar la acción - ${e.message}")
            e.printStackTrace()
            showErrorDialog("Error al guardar la acción: ${e.message}")
        }
    }


    // Cambiar el método abstracto para que devuelva el genérico T
    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T

    // Este método ahora solo carga los datos en la UI
    protected abstract fun loadActionData(action: Action?)

    // Este método ahora solo recolecta los datos de la UI y devuelve una nueva Acción con los datos actualizados
    protected abstract fun saveActionData(): Action

    protected fun notifyActionUpdated(updatedAction: Action){
        listener.onActionUpdated(updatedAction)
        Timber.d("${this.javaClass.simpleName}: Notified listener of action update for UUID: ${updatedAction.uuid}")
    }

    // Interfaz para que la Activity/Fragmento que muestra el diálogo reciba la acción actualizada
    interface ActionDialogListener {
        fun onActionUpdated(action: Action)
    }
}