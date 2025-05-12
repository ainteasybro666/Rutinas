package com.example.rutinas.ui.edit

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.FragmentRoutineEditBinding
import com.example.rutinas.ui.common.BaseFragment
import com.example.rutinas.ui.edit.actions.BaseEditActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditAlarmActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditAnnouncementActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditBrightnessActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditPauseActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditReadNotificationsActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditSoundModeActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditTimeActionDialogFragment
import com.example.rutinas.ui.edit.actions.EditVolumeActionDialogFragment
import com.example.rutinas.ui.edit.dialogs.CalendarTriggerDialog
import com.example.rutinas.ui.edit.dialogs.LocationTriggerDialog
import com.example.rutinas.ui.edit.dialogs.TimeTriggerConfigDialog
import com.example.rutinas.ui.edit.dialogs.TriggerTypeDialog
import com.example.rutinas.ui.main.adapter.ActionAdapter
import com.example.rutinas.ui.main.adapter.TriggerAdapter
import com.example.rutinas.utils.PermissionManager
import com.example.rutinas.utils.Resource
import com.example.rutinas.utils.showErrorSnackbar
import com.example.rutinas.utils.showSuccessSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import com.example.rutinas.ui.edit.dialogs.ActionCategoryDialog

// Importar NavController
import androidx.navigation.fragment.findNavController
// Importar navArgs
import androidx.navigation.fragment.navArgs

@AndroidEntryPoint
class RoutineEditFragment : BaseFragment(),
    TriggerTypeDialog.TriggerTypeListener,
    CalendarTriggerDialog.CalendarTriggerListener,
    LocationTriggerDialog.LocationTriggerListener,
    TimeTriggerConfigDialog.TimeTriggerConfigListener,
    ActionCategoryDialog.ActionSelectionListener, // Asegúrate de que esta interfaz está actualizada
    ActionDialogListener {

    @Inject
    lateinit var permissionManager: PermissionManager
    private val viewModel: RoutineEditViewModel by viewModels()
    private var currentRoutineId: Long = 0

    // Obtener los argumentos pasados al fragmento usando navArgs
    private val args:   RoutineEditFragmentArgs by navArgs()

    private lateinit var triggerAdapter: TriggerAdapter
    private lateinit var actionAdapter: ActionAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding {
        return FragmentRoutineEditBinding.inflate(inflater, container, false)
    }

    private val vb: FragmentRoutineEditBinding
        get() = binding as FragmentRoutineEditBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ocultar navegación y botón flotante al editar
        hideBottomNavMenu()
        hideFloatingButton()

        setupUI()
        setupTriggersRecyclerView()
        setupActionsRecyclerView()
        setupObservers()

        // Cargar datos si se está editando una rutina existente, o inicializar para una nueva
        // Usar args.routineUuid para obtener el UUID pasado por navegación
        val routineUuid = args.routineUuid
        Timber.d("Loading routine with UUID from navArgs: $routineUuid")
        viewModel.loadRoutine(routineUuid) // Pass the UUID (can be null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavMenu()
        showFloatingButton()
    }

    private fun setupUI() {
        with(vb) {
            toolbar.setNavigationIcon(R.drawable.ic_back)
            // Usar findNavController().popBackStack() para regresar en lugar de onBackPressed()
            toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            toolbar.title = "Editar Rutina"

            btnAddTrigger.setOnClickListener {
                if (canShowDialog()) { // Añadir verificación canShowDialog antes de mostrar
                    TriggerTypeDialog.createInstance().apply {
                        setTriggerTypeListener(this@RoutineEditFragment)
                        showDialog(this)
                    }
                }
            }

            btnAddAction.setOnClickListener {
                if (canShowDialog()) {
                    val actionCategoryDialog = ActionCategoryDialog.newInstance()
                    actionCategoryDialog.setActionSelectionListener(this@RoutineEditFragment)
                    actionCategoryDialog.show(childFragmentManager, "ActionCategoryDialog")
                }
            }
            btnSaveRoutine.setOnClickListener { saveRoutine() }
        }
    }

    private fun canShowDialog(): Boolean {
        // Simplified check
        return isAdded && context != null && !isStateSaved
    }

    private fun setupTriggersRecyclerView() {
        triggerAdapter = TriggerAdapter(
            onTriggerDeleted = { trigger ->
                viewModel.removeTrigger(trigger) // Delegate trigger removal to ViewModel
            }
        )

        vb.rvTriggers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = triggerAdapter
            addItemDecoration(createItemDecoration())
        }
    }

    private fun setupActionsRecyclerView() {
        actionAdapter = ActionAdapter(
            viewModel = viewModel,
            onActionDeleted = { action -> viewModel.removeAction(action) },
            onActionClicked = ::navigateToEditAction // Mantener esta llamada para editar acción
        ).apply {
            //attachTouchHelper(vb.rvActions)
        }

        vb.rvActions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = actionAdapter
        }
    }

    // Modified to correctly add action to ViewModel BEFORE opening the edit dialog
    // Asumiendo que ActionCategoryDialog.ActionSelectionListener ha sido modificado
    override fun onActionSelected(actionType: ActionType) { // Recibe ActionType
        Timber.d("ActionCategoryDialog: Action selected: $actionType")
        Timber.d("onActionSelected: currentRoutineId = $currentRoutineId") // Log currentRoutineId

        // 1. Crear una acción básica con valores por defecto
        val newAction = Action(
            id = 0L, // ID 0 para indicar que es nueva
            uuid = "", // UUID vacío, será generado por ViewModel
            actionType = actionType,
            routineId = currentRoutineId, // ID de la rutina actual (0 si es nueva)
            data = DataWrapper(emptyMap()), // Datos vacíos inicialmente
            executionOrder = viewModel.actions.value.size, // Orden al final de la lista actual
            pauseDuration = null // Duración de pausa por defecto
        )

        // 2. Añadir la acción al ViewModel (esto genera el UUID y la añade a la lista)
        //    ViewModel.addAction ahora devuelve la acción añadida con el UUID generado
        val addedActionWithUuid = viewModel.addAction(newAction)


        addedActionWithUuid?.let {
            // 4. Abrir el diálogo de configuración para esta acción con su UUID
            openEditActionDialog(it) // Pasa la acción con el UUID
        } ?: Timber.e("Error: No se pudo obtener la acción recién añadida del ViewModel.")
    }

    // Function to open the appropriate dialog fragment for an action
    private fun openEditActionDialog(action: Action) { // Recibe un Action con UUID
        Timber.d("Opening edit dialog for action: $action")
        val dialogFragment = when (action.actionType) {
            ActionType.ANNOUNCEMENT -> EditAnnouncementActionDialogFragment.newInstance(action, this)
            ActionType.ALARM -> EditAlarmActionDialogFragment.newInstance(action, this)
            ActionType.BRIGHTNESS -> EditBrightnessActionDialogFragment.newInstance(action, this)
            ActionType.PAUSE -> EditPauseActionDialogFragment.newInstance(action, this)
            ActionType.READ_NOTIFICATIONS -> EditReadNotificationsActionDialogFragment.newInstance(action, this)
            ActionType.SOUND_MODE -> EditSoundModeActionDialogFragment.newInstance(action, this)
            ActionType.TIME -> EditTimeActionDialogFragment.newInstance(action, this)
            ActionType.VOLUME -> EditVolumeActionDialogFragment.newInstance(action, this)
            else -> throw IllegalArgumentException("Unknown action type: ${action.actionType}")
        }
        showDialog(dialogFragment)
    }

    // Function to navigate to edit an *existing* action
    // This is called when an action in the list is clicked.
    private fun navigateToEditAction(action: Action) { // Receives an existing Action
        Timber.d("Navigating to edit existing action: $action")
        // Get the appropriate dialog fragment for this existing action
        val editFragment = getDialogFragmentForAction(action)
        // Show the dialog
        showDialog(editFragment) // Assuming showDialog handles parentFragmentManager or childFragmentManager correctly
    }

    // Function to get the appropriate dialog fragment for an action (used for editing existing actions)
    private fun getDialogFragmentForAction(action: Action): BaseEditActionDialogFragment {
        Timber.d("Getting dialog fragment for existing action: $action")
        // Ya no creamos una nueva acción aquí.
        // Siempre usamos la acción que nos llega, que debe tener un UUID si se llama desde navigateToEditAction.
        return when (action.actionType) {
            ActionType.ANNOUNCEMENT -> EditAnnouncementActionDialogFragment.newInstance(action, this)
            ActionType.ALARM -> EditAlarmActionDialogFragment.newInstance(action, this)
            ActionType.BRIGHTNESS -> EditBrightnessActionDialogFragment.newInstance(action, this)
            ActionType.PAUSE -> EditPauseActionDialogFragment.newInstance(action, this)
            ActionType.READ_NOTIFICATIONS -> EditReadNotificationsActionDialogFragment.newInstance(action, this)
            ActionType.SOUND_MODE -> EditSoundModeActionDialogFragment.newInstance(action, this)
            ActionType.TIME -> EditTimeActionDialogFragment.newInstance(action, this)
            ActionType.VOLUME -> EditVolumeActionDialogFragment.newInstance(action, this)
            else -> throw IllegalArgumentException("Unknown action type: ${action.actionType}")
        } as BaseEditActionDialogFragment
    }


    override fun onTriggerSelected(triggerType: TriggerTypeDialog.TriggerType) {
        if (!canShowDialog()) { // Usar canShowDialog aquí también
            Timber.e("onTriggerSelected: Cannot show dialog, fragment state invalid")
            return
        }

        val timeDialog = TimeTriggerConfigDialog.createInstance(currentRoutineId)
        val calendarDialog = CalendarTriggerDialog.createInstance(currentRoutineId)
        val locationDialog = LocationTriggerDialog.createInstance(currentRoutineId)

        when (triggerType) {
            TriggerTypeDialog.TriggerType.TIME -> {
                timeDialog.setTimeTriggerConfigListener(this@RoutineEditFragment)
                showDialog(timeDialog)
            }

            TriggerTypeDialog.TriggerType.CALENDAR -> {
                calendarDialog.setCalendarTriggerListener(this@RoutineEditFragment)
                showDialog(calendarDialog) // Usar showDialog helper
            }

            TriggerTypeDialog.TriggerType.LOCATION -> {
                locationDialog.setLocationTriggerListener(this@RoutineEditFragment)
                showDialog(locationDialog) // Usar showDialog helper
            }
        }
    }


    override fun onCalendarTriggerConfigured(trigger: Trigger) {
        Timber.d("Calendar trigger configured: $trigger")
        viewModel.addTrigger(trigger) // Delegate adding trigger to ViewModel
    }

    override fun onLocationTriggerConfigured(trigger: Trigger) {
        Timber.d("Location trigger configured: $trigger")
        viewModel.addTrigger(trigger) // Delegate adding trigger to ViewModel
    }

    override fun onTimeTriggerConfigured(trigger: Trigger) {
        Timber.d("Time trigger configured: $trigger")
        viewModel.addTrigger(trigger) // Delegate adding trigger to ViewModel
    }

    private fun setupObservers() {
        lifecycleScope.launch { // You are launching a coroutine in the lifecycleScope of the Fragment
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.d("Fragment: Inside repeatOnLifecycle(Lifecycle.State.STARTED)") // Existing Log: Confirma que el bloque se activa

                // Observe routine from ViewModel
                launch { // Launch a new coroutine within the repeatOnLifecycle block for routine
                    viewModel.currentRoutine.collect { routine ->
                        Timber.d("Fragment: Routine loaded/updated in observer: $routine")
                        routine?.let {
                            currentRoutineId = it.id
                            vb.etRoutineName.setText(it.name) // Update routine name in UI
                        }
                    }
                }


                Timber.d("Fragment: Setting up triggers and actions list observers") // Existing Log

                // Observe triggers from ViewModel
                launch { // Launch a new coroutine within the repeatOnLifecycle block for triggers
                    viewModel.triggers.collect { triggers ->
                        Timber.d("Trigger list updated: $triggers")
                        triggerAdapter.submitList(triggers.toList()) // Update the adapter
                        Timber.d("Fragment: Submitted ${triggers.size} triggers to adapter")
                    }
                }

                // Observe actions from ViewModel
                launch { // Launch a new coroutine within the repeatOnLifecycle block for actions
                    viewModel.actions.collect { actions ->
                        try { // NEW: Start try block
                            Timber.d("Action list updated: $actions") // Existing Log: Este log SÍ aparece
                            Timber.d("Fragment: Action list received in observer BEFORE submitList: ${actions.size} items") // Existing Log: Should now appear before submitList

                            // NEW Log: Check the current thread
                            Timber.d("Fragment: Current thread in actions collect block: ${Thread.currentThread().name}")

                            // NEW Log BEFORE submitList call
                            Timber.d("Fragment: Calling submitList with ${actions.size} items")
                            actionAdapter.submitList(actions.toList())
                            // NEW Log AFTER submitList call
                            Timber.d("Fragment: submitList called")

                            // This log might appear out of order depending on the thread execution, or might not be reached
                            // Timber.d("Fragment: Submitted ${actions.size} actions to adapter") // Keep if you want, but rely more on the logs around submitList

                            // NEW Log: Check the item count immediately after submitList
                            Timber.d("Fragment: ActionAdapter item count after submitList: ${actionAdapter.itemCount}")

                            // NEW Log: Mark the end of the try block execution
                            Timber.d("Fragment: End of actions collect block execution")

                        } catch (e: Exception) { // NEW: Catch any exception
                            Timber.e(e, "Fragment: Error in actions collect block") // NEW Log: Log the exception with stack trace
                        }
                    }
                }

                // Observe saveResult from ViewModel
                launch { // Launch a new coroutine within the repeatOnLifecycle block for saveResult
                    viewModel.saveResult.collect { result ->
                        when (result) {
                            is Resource.Success<Long> -> {
                                Timber.d("Routine saved successfully, ID: ${result.data}")
                                requireView().showSuccessSnackbar("Routine saved")
                                // Usar NavController para regresar al fragmento anterior
                                findNavController().popBackStack()
                            }

                            is Resource.Error -> {
                                Timber.e("Error saving routine: ${result.message}")
                                requireView().showErrorSnackbar(result.message ?: "Unknown error")
                            }

                            Resource.Loading -> { /* Show loader if you want */ }
                        }
                    }
                }
            }
        }
    }





    private fun saveRoutine() {
        val name = vb.etRoutineName.text.toString().trim()
        if (validateForm(name)) {
            Timber.d("Saving routine with name: $name, triggers: ${viewModel.triggers.value}, actions: ${viewModel.actions.value}")
            viewModel.saveRoutine(name = name) // Llama a saveRoutine sin pasar las listas
        }
    }

    private fun validateForm(name: String): Boolean {
        var isValid = true
        if (name.isEmpty()) {
            vb.etRoutineName.error = "Name required"
            isValid = false
        } // Use the list from the ViewModel for validation
        if (viewModel.triggers.value.isEmpty()) {
            requireView().showErrorSnackbar("Add at least one trigger")
            isValid = false
        }
        if (viewModel.actions.value.isNullOrEmpty()) {
            requireView().showErrorSnackbar("Add at least one action")
            isValid = false
        }
        return isValid
    }


    private fun createItemDecoration() = object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = 16.dpToPx()
        }
    }

    private fun Int.dpToPx(): Int =
        (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun hideBottomNavMenu() {
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.GONE
    }

    private fun showBottomNavMenu() {
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
    }

    private fun hideFloatingButton() {
        activity?.findViewById<View>(R.id.fab_add_routine)?.visibility = View.GONE
    }

    private fun showFloatingButton() {
        activity?.findViewById<View>(R.id.fab_add_routine)?.visibility = View.VISIBLE
    }

    // Helper function to show dialogs
    private fun showDialog(dialog: DialogFragment) {
        if (canShowDialog()) {
            // Use childFragmentManager for dialogs launched from this fragment
            dialog.show(childFragmentManager, dialog::class.java.simpleName)
        } else {
            Timber.e("Cannot show dialog: Invalid fragment state or isStateSaved")
        }
    }

    override fun onActionUpdated(updatedAction: Action) {
        Timber.d("onActionUpdated received: $updatedAction")
        viewModel.updateAction(updatedAction)
    }
}
