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

    // Variables para la gestión de triggers y acciones
    private val triggersList = mutableListOf<Trigger>()
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
        val routineUuid = arguments?.getString("routine_uuid")
        Timber.d("Loading routine with UUID: $routineUuid")
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
            toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            toolbar.title = "Editar Rutina"

            btnAddTrigger.setOnClickListener {
                TriggerTypeDialog.createInstance().apply {
                    setTriggerTypeListener(this@RoutineEditFragment)
                    showDialog(this)
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
        return isAdded && !isDetached && !isRemoving && context != null
    }

    private fun setupTriggersRecyclerView() {
        triggerAdapter = TriggerAdapter(
            onTriggerDeleted = { trigger ->
                triggersList.remove(trigger)
                triggerAdapter.submitList(triggersList.toList())
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
            onActionClicked = ::navigateToEditAction
        ).apply {
            attachTouchHelper(vb.rvActions)
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
        viewModel.addAction(newAction)

        // 3. Obtener la acción del ViewModel con el UUID generado.
        //    Idealmente, addAction en el ViewModel devolvería la acción añadida.
        //    Por ahora, asumimos que es la última añadida.
        val addedAction = viewModel.actions.value.lastOrNull()

        addedAction?.let {
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
        if (!isAdded || isDetached) {
            Timber.e("onTriggerSelected: Fragment not attached")
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
                if (isAdded) calendarDialog.show(childFragmentManager, "CalendarTriggerDialog")
            }

            TriggerTypeDialog.TriggerType.LOCATION -> {
                locationDialog.setLocationTriggerListener(this@RoutineEditFragment)
                if (isAdded) locationDialog.show(childFragmentManager, "LocationTriggerDialog")
            }
        }
    }


    override fun onCalendarTriggerConfigured(trigger: Trigger) {
        Timber.d("Calendar trigger configured: $trigger")
        triggersList.add(trigger)
        triggerAdapter.submitList(triggersList.toList())
    }

    override fun onLocationTriggerConfigured(trigger: Trigger) {
        Timber.d("Location trigger configured: $trigger")
        triggersList.add(trigger)
        triggerAdapter.submitList(triggersList.toList())
    }

    override fun onTimeTriggerConfigured(trigger: Trigger) {
        Timber.d("Time trigger configured: $trigger")
        triggersList.add(trigger)
        triggerAdapter.submitList(triggersList.toList())
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentRoutine.collect { routine ->
                    Timber.d("Routine loaded/updated: $routine")
                    routine?.let {
                        currentRoutineId = it.id
                        vb.etRoutineName.setText(it.name)
                        triggersList.clear()
                        triggersList.addAll(it.triggers)
                        triggerAdapter.submitList(triggersList.toList()) // Usar submitList con una copia
                    }
                }
            }

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.actions.collect { actions ->
                        Timber.d("Action list updated: $actions")
                        // Asegurarse de que la lista enviada es una nueva instancia para DiffUtil
                        actionAdapter.submitList(actions.toList())
                    }
                }
            }

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.saveResult.collect { result ->
                        when (result) {
                            is Resource.Success<Long> -> {
                                Timber.d("Routine saved successfully, ID: ${result.data}")
                                requireView().showSuccessSnackbar("Routine saved")
                                requireActivity().onBackPressedDispatcher.onBackPressed()
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
            Timber.d("Saving routine with name: $name, triggers: $triggersList, actions: ${viewModel.actions.value}")
            viewModel.saveRoutine(name = name) // Llama a saveRoutine sin pasar las listas
        }
    }

    private fun validateForm(name: String): Boolean {
        var isValid = true
        if (name.isEmpty()) {
            vb.etRoutineName.error = "Name required"
            isValid = false
        }
        if (triggersList.isEmpty()) {
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

    private fun showDialog(dialog: DialogFragment) {
        if (canShowDialog()) {
            dialog.show(parentFragmentManager, dialog::class.java.simpleName)
        } else {
            Timber.e("Cannot show dialog: Invalid fragment state")
        }
    }

    override fun onActionUpdated(updatedAction: Action) {
        viewModel.updateAction(updatedAction)
    }
}