package com.example.rutinas.ui.edit

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.rutinas.ui.edit.dialogs.TriggerTypeDialog.TriggerType

@AndroidEntryPoint
class RoutineEditFragment : BaseFragment(),
    TriggerTypeDialog.TriggerTypeListener,
    CalendarTriggerDialog.CalendarTriggerListener,
    LocationTriggerDialog.LocationTriggerListener,
    TimeTriggerConfigDialog.TimeTriggerConfigListener,
    ActionCategoryDialog.ActionSelectionListener,
    BaseEditActionDialogFragment.ActionDialogListener {

    @Inject
    lateinit var permissionManager: PermissionManager
    private val viewModel: RoutineEditViewModel by viewModels()
    // private var currentRoutineId: Long = 0 // Ya no es necesario almacenar el ID aquí si usas el StateFlow de la rutina

    // Obtener los argumentos pasados al fragmento usando navArgs
    private val args: RoutineEditFragmentArgs by navArgs()

    private lateinit var triggerAdapter: TriggerAdapter
    private lateinit var actionAdapter: ActionAdapter

    // Variable para rastrear la acción que acabamos de añadir y para la que necesitamos abrir el diálogo.
    // La inicializamos a null. Se establecerá cuando se selecciona un tipo de acción en el ActionCategoryDialog
    // y se limpiará después de mostrar el diálogo.
    private var actionToConfigure: Action? = null

    // Declara las instancias de tus diálogos
    private lateinit var timeTriggerConfigDialog: TimeTriggerConfigDialog
    private lateinit var calendarTriggerDialog: CalendarTriggerDialog


    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding {
        Timber.d("RoutineEditFragment: Inflating binding")
        return FragmentRoutineEditBinding.inflate(inflater, container, false)
    }

    private val vb: FragmentRoutineEditBinding
        get() = binding as FragmentRoutineEditBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("RoutineEditFragment: onViewCreated() called")

        // Ocultar navegación y botón flotante al editar (métodos de BaseFragment)
        hideBottomNavMenu()
        hideFloatingButton()

        setupUI()
        setupTriggersRecyclerView()
        setupActionsRecyclerView()
//        setupObservers() // Aquí configuramos los observadores

        // Cargar datos si se está editando una rutina existente, o inicializar para una nueva
        val routineUuid = args.routineUuid
        Timber.d("RoutineEditFragment: Loading routine with UUID from navArgs: $routineUuid")
        viewModel.loadRoutine(routineUuid) // Pass the UUID (can be null for new routine)

        // Configurar la Toolbar del fragmento (VISUALMENTE, NO como ActionBar)
        vb.toolbar.title = if (routineUuid == null) "Nueva Rutina" else "Editar Rutina"
        vb.toolbar.navigationIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)
        vb.toolbar.setNavigationOnClickListener {
            Timber.d("RoutineEditFragment: Back button clicked on fragment toolbar. Navigating up.")
            findNavController().navigateUp() // Navegar hacia arriba
        }
        Timber.d("RoutineEditFragment: Fragment Toolbar setup complete.")

        //Triggers
//        timeTriggerConfigDialog = TimeTriggerConfigDialog()
//        calendarTriggerDialog = CalendarTriggerDialog()

        // Cargar la rutina cuando la vista es creada
        viewModel.loadRoutine(args.routineUuid) // Usa el UUID de los argumentos de navegación

        // Observar el StateFlow de triggers en el ViewModel para actualizar la UI
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.triggers.collect { triggers ->
                    // Aquí deberías actualizar tu RecyclerView o la vista que muestra los triggers
                    // Por ejemplo, si usas un Adapter:
                    // triggerAdapter.submitList(triggers)
                    Timber.d("UI: Lista de triggers actualizada. Total: ${triggers.size}")
                }
            }
        }

        // Observar el StateFlow de acciones en el ViewModel para actualizar la UI
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actions.collect { actions ->
                    // Aquí deberías actualizar tu RecyclerView o la vista que muestra las acciones
                    // Por ejemplo, si usas un Adapter:
                    // actionAdapter.submitList(actions)
                    Timber.d("UI: Lista de acciones actualizada. Total: ${actions.size}")
                }
            }
        }

        // Observar los eventos de UI del ViewModel (mensajes de error, etc.)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    Timber.d("RoutineEditFragment: Received UI event: $event") // Log para ver qué eventos llegan
                    when (event) {
                        is RoutineEditViewModel.UiEvent.ShowMessage -> { // Asegúrate de que UiEvent es accesible (puede ser inner class o en otro archivo)
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        // Añade aquí el manejo de otros tipos de eventos de UI si los defines en el ViewModel
                        // Por ejemplo:
                        // is RoutineEditViewModel.UiEvent.NavigateBack -> findNavController().popBackStack()
                        else -> {
                            // Esto es importante si UiEvent es sealed. Cubre cualquier tipo no manejado explícitamente.
                            Timber.w("RoutineEditFragment: Received unhandled UiEvent: $event")
                        }
                    }
                }
            }
        }



        // Configura los botones o UI para mostrar los diálogos de triggers
        // Por ejemplo, si tienes un botón para añadir trigger:
        // Usando el ID del layout: btnAddTrigger
        vb.btnAddTrigger.setOnClickListener {
            Timber.d("btnAddTrigger clicked. Showing TriggerTypeDialog.")
            val triggerTypeDialog = TriggerTypeDialog()

            // PASO CORRECTO: Configurar el Fragment como listener del diálogo de selección de tipo
            // Asegúrate de que RoutineEditFragment implementa TriggerTypeDialog.TriggerTypeListener
            // y que has definido el méto-do override fun onTriggerSelected(...)
            triggerTypeDialog.setTriggerTypeListener(this@RoutineEditFragment) // <-- Pasa la instancia del Fragment

            // PASO CORRECTO: Mostrar el diálogo
            showDialog(triggerTypeDialog)
        }

        // Configura el botón de guardar
        // Usando el ID del layout: btnSaveRoutine
        vb.btnSaveRoutine.setOnClickListener { // **ADAPTADO**
            // Usando el ID del layout para el EditText: etRoutineName
            val routineName = vb.etRoutineName.text.toString().trim() // **ADAPTADO**
            if (routineName.isEmpty()) {
                Toast.makeText(requireContext(), "El nombre de la rutina no puede estar vacío.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveRoutine(routineName)
        }

        // Observar el resultado de guardar la rutina
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveResult.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Mostrar un indicador de carga
                            Timber.d("UI: Guardando rutina...")
                        }
                        is Resource.Success -> {
                            // Rutina guardada con éxito. Puedes navegar de regreso o mostrar un mensaje.
                            Toast.makeText(requireContext(), "Rutina guardada con éxito.", Toast.LENGTH_SHORT).show()
                            Timber.d("UI: Rutina guardada con éxito, ID: ${resource.data}")
                            // Opcional: findNavController().popBackStack() // Navegar de regreso
                        }
                        is Resource.Error -> {
                            // Mostrar el mensaje de error
                            Toast.makeText(requireContext(), "Error al guardar rutina: ${resource.message}", Toast.LENGTH_LONG).show()
                            Timber.e("UI: Error al guardar rutina: ${resource.message}")
                        }
                    }
                }
            }
        }
    }

    // ELIMINAR: Ya no desconfiguramos la ActionBar de la Activity en onDestroyView
    // override fun onDestroyView() { ... }

    private fun setupUI() {
        with(vb) {
            btnAddTrigger.setOnClickListener {
                if (canShowDialog()) {
                    Timber.d("RoutineEditFragment: Add Trigger button clicked. Showing TriggerTypeDialog.")
                    TriggerTypeDialog.createInstance().apply {
                        setTriggerTypeListener(this@RoutineEditFragment)
                        showDialog(this) // Usar el helper showDialog
                    }
                } else {
                    Timber.w("RoutineEditFragment: Cannot show TriggerTypeDialog, fragment state invalid.")
                }
            }

            btnAddAction.setOnClickListener {
                if (canShowDialog()) {
                    Timber.d("RoutineEditFragment: Add Action button clicked. Showing ActionCategoryDialog.")
                    val actionCategoryDialog = ActionCategoryDialog.newInstance()
                    actionCategoryDialog.setActionSelectionListener(this@RoutineEditFragment)
                    actionCategoryDialog.show(childFragmentManager, "ActionCategoryDialog")
                } else {
                    Timber.w("RoutineEditFragment: Cannot show ActionCategoryDialog, fragment state invalid.")
                }
            }
            btnSaveRoutine.setOnClickListener {
                Timber.d("RoutineEditFragment: Save Routine button clicked. Calling saveRoutine().")
                saveRoutine()
            }
        }
        Timber.d("RoutineEditFragment: UI setup complete.")
    }

    private fun canShowDialog(): Boolean {
        return isAdded && context != null && !isStateSaved && !childFragmentManager.isStateSaved
    }

    private fun setupTriggersRecyclerView() {
        Timber.d("RoutineEditFragment: Setting up Triggers RecyclerView")
        // Corregida la llamada al constructor de TriggerAdapter.
        // Asegúrate de que el constructor de tu TriggerAdapter acepta onTriggerDeleted y onTriggerClicked.
        triggerAdapter = TriggerAdapter(
            onTriggerDeleted = { trigger ->
                Timber.d("RoutineEditFragment: Trigger deleted via adapter click: ${trigger.uuid}")
                viewModel.removeTrigger(trigger)
            },
            onTriggerClicked = { trigger ->
                Timber.d("RoutineEditFragment: Trigger clicked via adapter: ${trigger.uuid}. TODO: Implementar diálogo de edición aquí.")
                // TODO: Implementar lógica para mostrar el diálogo de edición de trigger apropiado
                // openEditTriggerDialog(trigger) // Esto es un ejemplo de cómo lo llamarías si creas esa función
            }
        )

        vb.rvTriggers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = triggerAdapter
            addItemDecoration(createItemDecoration())
        }
        Timber.d("RoutineEditFragment: Triggers RecyclerView setup complete.")
    }

    private fun setupActionsRecyclerView() {
        Timber.d("RoutineEditFragment: Setting up Actions RecyclerView")
        actionAdapter = ActionAdapter(
            viewModel = viewModel,
            onActionDeleted = { action ->
                Timber.d("RoutineEditFragment: Action deleted via adapter click: ${action.uuid}")
                viewModel.removeAction(action)
            },
            onActionClicked = ::navigateToEditAction // Llama a navigateToEditAction para editar
        ).apply {
            // attachTouchHelper(vb.rvActions) // Comentado
        }

        vb.rvActions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = actionAdapter
            addItemDecoration(createItemDecoration())
        }
        Timber.d("RoutineEditFragment: Actions RecyclerView setup complete.")
    }


    // *** Implementación crucial: Observador de acciones para abrir el diálogo ***
    private fun setupObservers() {
        Timber.d("RoutineEditFragment: Setting up ViewModel Observers")
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Timber.d("RoutineEditFragment: Inside repeatOnLifecycle(Lifecycle.State.STARTED) observer block")

                // Observe routine from ViewModel
                launch {
                    viewModel.currentRoutine.collect { routine ->
                        Timber.d("RoutineEditFragment: Routine loaded/updated in observer: $routine")
                        routine?.let {
                            // Ya no es necesario asignar a currentRoutineId si usas directamente el StateFlow en saveRoutine
                            // currentRoutineId = it.id
                            vb.etRoutineName.setText(it.name)
                        }
                    }
                }

                // Observe triggers from ViewModel
                launch {
                    viewModel.triggers.collect { triggers ->
                        Timber.d("RoutineEditFragment: Trigger list updated in observer: ${triggers.size} items")
                        triggerAdapter.submitList(triggers.toList())
                        Timber.d("RoutineEditFragment: Submitted ${triggers.size} triggers to adapter")
                    }
                }

                // Observe actions from ViewModel
                launch {
                    viewModel.actions.collect { actions ->
                        Timber.d("RoutineEditFragment: Action list updated in observer: ${actions.size} items")
                        // Log para verificar qué acciones se reciben
                        Timber.d("RoutineEditFragment: Actions received: ${actions.map { it.uuid to it.actionType }}")

                        val currentListBeforeUpdate =
                            actionAdapter.currentList.toList() // Copia la lista actual del adapter
                        actionAdapter.submitList(actions.toList()) { // Usa la sobrecarga con Runnable de completado
                            Timber.d("RoutineEditFragment: ActionAdapter submitList completed.")

                            // Lógica para encontrar la acción recién añadida y abrir su diálogo
                            // Esto solo debe ocurrir si tenemos una acción pendiente de configuración
                            // Y la nueva lista del ViewModel contiene esa acción.
                            actionToConfigure?.let { pendingAction ->
                                Timber.d("RoutineEditFragment: Checking for pending action to configure: UUID ${pendingAction.uuid}")
                                // Buscar la acción pendiente en la lista ACTUALIZADA del ViewModel
                                val configuredAction =
                                    actions.find { it.uuid == pendingAction.uuid }

                                if (configuredAction != null) {
                                    Timber.d("RoutineEditFragment: Found pending action with UUID ${pendingAction.uuid} in updated list.")
                                    // Abrir el diálogo de configuración para esta acción
                                    openEditActionDialog(configuredAction)
                                    // Limpiar la variable después de abrir el diálogo
                                    actionToConfigure = null
                                    Timber.d("RoutineEditFragment: Config dialog opened, actionToConfigure cleared.")
                                } else {
                                    Timber.d("RoutineEditFragment: Pending action with UUID ${pendingAction.uuid} not found in updated list yet.")
                                    // Esto podría ocurrir si submitList aún no ha procesado la actualización,
                                    // o si hay un retraso en la propagación del StateFlow.
                                    // La variable actionToConfigure persistirá hasta la próxima actualización.
                                }
                            } ?: run {
                                Timber.d("RoutineEditFragment: No actionToConfigure is pending.")
                            }
                        }
                        Timber.d("RoutineEditFragment: Finished processing actions collect block.")
                    }
                }

                // Observe saveResult from ViewModel
                launch {
                    viewModel.saveResult.collect { result ->
                        when (result) {
                            is Resource.Success<Long> -> {
                                Timber.d("RoutineEditFragment: Routine saved successfully, ID: ${result.data}")
                                requireView().showSuccessSnackbar("Routine saved")
                                // Usar NavController para regresar al fragmento anterior
                                findNavController().popBackStack()
                            }

                            is Resource.Error -> {
                                Timber.e("RoutineEditFragment: Error saving routine: ${result.message}")
                                requireView().showErrorSnackbar(result.message ?: "Unknown error")
                            }

                            Resource.Loading -> { /* Show loader if you want */
                                Timber.d("RoutineEditFragment: Save routine result: Loading")
                            }

                            else -> {
                                Timber.d("RoutineEditFragment: Save routine result: Idle or other state")
                            }
                        }
                    }
                }
            }
        }
        Timber.d("RoutineEditFragment: ViewModel Observers setup complete.")
    }


    // Modified onActionSelected to set actionToConfigure
    override fun onActionSelected(actionType: ActionType) {
        Timber.d("RoutineEditFragment: onActionSelected called with action type: $actionType")
        if (!canShowDialog()) {
            Timber.w("RoutineEditFragment: Cannot proceed with action selection, fragment state invalid.")
            return
        }

        // 1. Crear una acción básica con valores por defecto.
        //    El UUID y el ID serán manejados por el ViewModel al añadirla.
        //    Asociamos a la rutina actual si ya está cargada (ViewModel.currentRoutine.value?.id)
        //    o usamos 0L si es una nueva rutina.
        val newAction = Action(
            actionType = actionType,
            // Asigna el ID de la rutina actual si está disponible, de lo contrario 0L.
            // El ViewModel debe ser capaz de manejar 0L para nuevas rutinas.
            routineId = viewModel.currentRoutine.value?.id ?: 0L,
            data = DataWrapper(emptyMap()),
            executionOrder = viewModel.actions.value.size,
            pauseDuration = null
        )
        Timber.d("RoutineEditFragment: Created new action object: $newAction")

        // *** Establecer la acción pendiente de configuración ANTES de añadirla al ViewModel ***
        // Esto es CRUCIAL para que el observer la detecte después de que el ViewModel la procese.
        actionToConfigure =
            newAction.copy() // Usa copy para asegurar que es una instancia diferente si es necesario

        // 2. Añadir la acción al ViewModel.
        viewModel.addAction(newAction)
        Timber.d("RoutineEditFragment: New action added to ViewModel and actionToConfigure set. Observer will handle dialog.")
    }


    override fun onActionUpdated(updatedAction: Action) {
        Timber.d("RoutineEditFragment: onActionUpdated received for UUID: ${updatedAction.uuid}")
        viewModel.updateAction(updatedAction)
        Timber.d("RoutineEditFragment: Action updated in ViewModel.")
    }


    private fun saveRoutine() {
        val name = vb.etRoutineName.text.toString().trim()
        // Usar los StateFlows del ViewModel para la validación
        if (validateForm(name, viewModel.triggers.value, viewModel.actions.value)) {
            Timber.d("RoutineEditFragment: Saving routine with name: $name, triggers count: ${viewModel.triggers.value.size}, actions count: ${viewModel.actions.value.size}")
            // Llama a saveRoutine en el ViewModel. El ViewModel usará sus StateFlows internos.
            viewModel.saveRoutine(name = name)
            Timber.d("RoutineEditFragment: viewModel.saveRoutine() called.")
        } else {
            Timber.w("RoutineEditFragment: Routine validation failed.")
        }
    }

    // Modified validateForm to accept trigger and action lists
    private fun validateForm(
        name: String,
        triggers: List<Trigger>,
        actions: List<Action>
    ): Boolean {
        Timber.d("RoutineEditFragment: Validating form. Name: '$name', Triggers: ${triggers.size}, Actions: ${actions.size}")
        var isValid = true
        if (name.isEmpty()) {
            vb.etRoutineName.error =
                "Name required" // Asegúrate de que este string está en resources
            isValid = false
            Timber.d("RoutineEditFragment: Validation failed: Name is empty.")
        }
        if (triggers.isEmpty()) {
            requireView().showErrorSnackbar("Add at least one trigger") // Asegúrate de que este string está en resources
            isValid = false
            Timber.d("RoutineEditFragment: Validation failed: No triggers.")
        }
        if (actions.isEmpty()) {
            requireView().showErrorSnackbar("Add at least one action") // Asegúrate de que este string está en resources
            isValid = false
            Timber.d("RoutineEditFragment: Validation failed: No actions.")
        }
        Timber.d("RoutineEditFragment: Form validation result: $isValid")
        return isValid
    }

    // Helper functions (createItemDecoration, dpToPx, hide/show bottom nav/fab, showDialog)
    // Se mantienen como las proporcionaste.
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
        Timber.d("RoutineEditFragment: Hiding bottom navigation menu.")
    }

    private fun showBottomNavMenu() {
        activity?.findViewById<View>(R.id.bottomNavigation)?.visibility = View.VISIBLE
        Timber.d("RoutineEditFragment: Showing bottom navigation menu.")
    }

    private fun hideFloatingButton() {
        activity?.findViewById<View>(R.id.fab_add_routine)?.visibility = View.GONE
        Timber.d("RoutineEditFragment: Hiding floating action button.")
    }

    private fun showFloatingButton() {
        activity?.findViewById<View>(R.id.fab_add_routine)?.visibility = View.VISIBLE
        Timber.d("RoutineEditFragment: Showing floating action button.")
    }

    // Helper function to show dialogs
    private fun showDialog(dialog: DialogFragment) {
        if (canShowDialog()) {
            // Use childFragmentManager for dialogs launched from this fragment
            Timber.d("RoutineEditFragment: Attempting to show dialog: ${dialog::class.java.simpleName}")
            try {
                dialog.show(childFragmentManager, dialog::class.java.simpleName)
                Timber.d("RoutineEditFragment: Dialog ${dialog::class.java.simpleName} shown successfully.")
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "RoutineEditFragment: Error showing dialog ${dialog::class.java.simpleName}"
                )
                // Opcional: Mostrar un Toast o Snackbar al usuario en caso de error
                Toast.makeText(requireContext(), "Error displaying dialog", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Timber.e("RoutineEditFragment: Cannot show dialog ${dialog::class.java.simpleName}: Invalid fragment state or isStateSaved")
            Toast.makeText(requireContext(), "Cannot open dialog at this time", Toast.LENGTH_SHORT)
                .show()
        }
    }


    // Implementaciones de los listeners de diálogos de triggers (se mantuvieron de la parte 2)
    // Estos métodos se llaman cuando se selecciona un tipo de trigger en el TriggerTypeDialog
    // y luego se configura en su diálogo específico.
    // Asegúrate de que TriggerTypeDialog.TriggerType es el tipo correcto que esperas.
    override fun onTriggerSelected(triggerType: TriggerTypeDialog.TriggerType) {
        Timber.d("RoutineEditFragment: Trigger type selected: $triggerType")
        when(triggerType) {
            TriggerTypeDialog.TriggerType.TIME -> { // Usa el enum directamente
                val timeDialog = TimeTriggerConfigDialog.createInstance(viewModel.currentRoutine.value?.id ?: 0L)
                timeDialog.setTimeTriggerConfigListener(this@RoutineEditFragment)
                showDialog(timeDialog)
            }
            TriggerTypeDialog.TriggerType.CALENDAR -> { // Usa el enum directamente
                val calendarDialog = CalendarTriggerDialog.createInstance(viewModel.currentRoutine.value?.id ?: 0L)
                calendarDialog.setCalendarTriggerListener(this@RoutineEditFragment)
                showDialog(calendarDialog)
            }
            TriggerTypeDialog.TriggerType.LOCATION -> { // Usa el enum directamente
                val locationDialog = LocationTriggerDialog.createInstance(viewModel.currentRoutine.value?.id ?: 0L)
                locationDialog.setLocationTriggerListener(this@RoutineEditFragment)
                showDialog(locationDialog)
            }
            else -> {
                Timber.w("RoutineEditFragment: Unhandled TriggerType selected: $triggerType")
                Toast.makeText(requireContext(), "Tipo de trigger no soportado", Toast.LENGTH_SHORT).show()
            }
        }
        Timber.d("RoutineEditFragment: Trigger config dialog shown for type: $triggerType.")
    }

    // Implementaciones de los listeners de configuración de triggers.
    // Estos métodos se llaman cuando se configura y guarda un trigger en su diálogo específico.
    // Nombre de método corregido según tu código original.
    override fun onCalendarTriggerConfigured(trigger: Trigger) {
        // Asigna el routineId correcto si la rutina ya existe (después del primer guardado)
        val currentRoutineId = viewModel.currentRoutine.value?.id ?: 0L // Obtiene el ID actual
        val triggerWithRoutineId = if (currentRoutineId > 0) {
            trigger.copy(routineId = currentRoutineId)
        } else {
            trigger // Si la rutina es nueva, el routineId se asignará al guardar
        }

        when (val result = viewModel.addTrigger(triggerWithRoutineId)) {
            is Resource.Success -> {
                // Trigger añadido correctamente. Puedes actualizar la UI si es necesario.
                Timber.d("Trigger de calendario añadido correctamente en la UI.")
                // Aquí podrías notificar a tu adaptador de triggers
            }

            is Resource.Error -> {
                // Mostrar el mensaje de error al usuario
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                Timber.w("Error al añadir trigger de calendario: ${result.message}")
            }

            is Resource.Loading -> {
                // Opcional: mostrar un indicador de carga
            }
        }
    }

    // Nombre de método corregido según tu código original.
    override fun onLocationTriggerConfigured(trigger: Trigger) {
        Timber.d("RoutineEditFragment: Received configured Location trigger in listener: ${trigger.uuid}")
        viewModel.addTrigger(trigger)
        Timber.d("RoutineEditFragment: Location trigger added to ViewModel.")
    }

    override fun onTimeTriggerConfigured(trigger: Trigger) {
        Timber.d("RoutineEditFragment: onTimeTriggerConfigured received trigger: $trigger") // Log para depuración
        // Asigna el routineId correcto si la rutina ya existe (después del primer guardado)
        val currentRoutineId = viewModel.currentRoutine.value?.id ?: 0L // Obtiene el ID actual
        val triggerWithRoutineId = if (currentRoutineId > 0) {
            trigger.copy(routineId = currentRoutineId)
        } else {
            trigger // Si la rutina es nueva, el routineId se asignará al guardar
        }

        when (val result = viewModel.addTrigger(triggerWithRoutineId)) {
            is Resource.Success -> {
                // Trigger añadido correctamente. Puedes actualizar la UI si es necesario.
                Timber.d("Trigger de tiempo añadido correctamente en la UI.")
                // Aquí podrías notificar a tu adaptador de triggers si usas uno
                // para que la lista en la UI se actualice.
            }

            is Resource.Error -> {
                // Mostrar el mensaje de error al usuario
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                Timber.w("Error al añadir trigger de tiempo: ${result.message}")
            }

            is Resource.Loading -> {
                // Opcional: mostrar un indicador de carga si la operación fuera asíncrona
            }
        }

        viewModel.saveTrigger(trigger)
        //dialog?.dismiss() // Si tu helper showDialog te permite obtener la instancia
    }

    // Métodos navigateToEditAction y openEditActionDialog se mantienen de la parte 2.
    // Se llaman desde el adapter y desde el observer, respectivamente.
    private fun navigateToEditAction(action: Action) {
        Timber.d("RoutineEditFragment: navigateToEditAction called for existing action UUID: ${action.uuid}")
        openEditActionDialog(action)
    }

    private fun openEditActionDialog(action: Action) {
        Timber.d("RoutineEditFragment: openEditActionDialog called for action UUID: ${action.uuid}")
        if (!canShowDialog()) {
            Timber.e("RoutineEditFragment: Cannot show edit action dialog, fragment state invalid.")
            return
        }

        val dialogFragment: DialogFragment = when (action.actionType) {
            ActionType.ALARM -> EditAlarmActionDialogFragment.newInstance(action, this)
            ActionType.ANNOUNCEMENT -> EditAnnouncementActionDialogFragment.newInstance(action, this)
            ActionType.BRIGHTNESS -> EditBrightnessActionDialogFragment.newInstance(action, this)
            ActionType.PAUSE -> EditPauseActionDialogFragment.newInstance(action, this)
            ActionType.READ_NOTIFICATIONS -> EditReadNotificationsActionDialogFragment.newInstance(action, this)
            ActionType.SOUND_MODE -> EditSoundModeActionDialogFragment.newInstance(action, this)
            ActionType.TIME -> EditTimeActionDialogFragment.newInstance(action, this)
            ActionType.VOLUME -> EditVolumeActionDialogFragment.newInstance(action, this) // Tu diálogo de volumen
            else -> {
                Timber.e("RoutineEditFragment: Unknown action type for editing dialog: ${action.actionType}")
                // Considerar mostrar un Toast o Snackbar aquí también.
                throw IllegalArgumentException("Unknown action type: ${action.actionType}")
            }
        }
        showDialog(dialogFragment) // Usar el helper showDialog
        Timber.d("RoutineEditFragment: Edit dialog shown for action UUID: ${action.uuid}")
    }



    // ELIMINADO: getDialogFragmentForAction ya no es necesario.
    // private fun getDialogFragmentForAction(action: Action): BaseEditActionDialogFragment<*> { ... }
}