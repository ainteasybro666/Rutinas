package com.example.rutinas.ui.edit

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rutinas.R
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.Trigger
import com.example.rutinas.databinding.FragmentRoutineEditBinding
import com.example.rutinas.ui.edit.actions.*
import com.example.rutinas.ui.edit.dialogs.*
import com.example.rutinas.ui.main.adapter.ActionAdapter
import com.example.rutinas.ui.main.adapter.TriggerAdapter
import com.example.rutinas.utils.PermissionManager
import com.example.rutinas.utils.Resource
import com.example.rutinas.utils.showErrorSnackbar
import com.example.rutinas.utils.showSuccessSnackbar
import com.example.rutinas.ui.edit.dialogs.TriggerTypeDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.rutinas.ui.common.BaseFragment

@AndroidEntryPoint
class RoutineEditFragment : BaseFragment(),
    TriggerTypeDialog.TriggerTypeListener,
    CalendarTriggerDialog.CalendarTriggerListener,
    LocationTriggerDialog.LocationTriggerListener,
    TimeTriggerConfigDialog.TimeTriggerConfigListener {

    @Inject lateinit var permissionManager: PermissionManager
    private val viewModel: RoutineEditViewModel by viewModels()

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

        // Cargar datos si se está editando una rutina existente
        arguments?.getString("routine_uuid")?.let { uuid ->
            Timber.d("Cargando rutina con UUID: $uuid")
            viewModel.loadRoutine(uuid)
        }
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
                    showAddActionDialog()
                }
            }
            btnSaveRoutine.setOnClickListener { saveRoutine() }
        }
    }

    private fun canShowDialog(): Boolean {
        return isAdded && !isDetached && !isRemoving && context != null
    }

    private fun showAddActionDialog() {
        if (!canShowDialog()) {
            Timber.e("No se puede mostrar diálogo: Estado inválido del fragmento")
            return
        }

        AddActionDialogFragment.newInstance().apply {
            setOnActionSelectedListener { navigateToEditAction(it) }
            showDialog(this)
        }
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
    private fun navigateToEditAction(action: Action) {
        val editFragment: DialogFragment = when (action.type) {
            ActionType.ALARM -> {
                EditAlarmActionDialogFragment.newInstance(action)
            }
            ActionType.ANNOUNCEMENT -> {
                EditAnnouncementActionDialogFragment.newInstance(action)
            }
            ActionType.BRIGHTNESS -> EditBrightnessActionDialogFragment.newInstance(action)
            ActionType.VOLUME -> EditVolumeActionDialogFragment.newInstance(action)
            ActionType.SOUND_MODE -> EditSoundModeActionDialogFragment.newInstance(action)
            ActionType.TIME -> EditTimeActionDialogFragment.newInstance(action)
            ActionType.READ_NOTIFICATIONS -> EditReadNotificationsActionDialogFragment.newInstance(action)
            ActionType.PAUSE -> EditPauseActionDialogFragment.newInstance(action)
            else -> {
                throw IllegalArgumentException("Tipo no soportado: ${'$'}{action.type}")
            }
        }

        (editFragment as? BaseEditActionDialogFragment)?.setOnActionUpdatedListener { updatedAction ->
            viewModel.updateAction(updatedAction)
        }
        editFragment.show(childFragmentManager, "EditActionDialog")
    }

    override fun onTriggerSelected(triggerType: TriggerTypeDialog.TriggerType) {
        if (!isAdded || isDetached) {
            Timber.e("onTriggerSelected: Fragment no adjunto")
            return
        }

        val timeDialog = TimeTriggerConfigDialog.createInstance()
        val calendarDialog = CalendarTriggerDialog.createInstance()
        val locationDialog = LocationTriggerDialog.createInstance()

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
        Timber.d("Trigger de calendario configurado: $trigger")
        triggersList.add(trigger)
        triggerAdapter.submitList(triggersList.toList())
    }

    override fun onLocationTriggerConfigured(trigger: Trigger) {
        Timber.d("Trigger de ubicación configurado: $trigger")
        triggersList.add(trigger)
        triggerAdapter.submitList(triggersList.toList())
    }

    override fun onTimeTriggerConfigured(trigger: Trigger) {
        Timber.d("Trigger de tiempo configurado: $trigger")
        triggersList.add(trigger)
        triggerAdapter.submitList(triggersList.toList())
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentRoutine.collect { routine ->
                    Timber.d("Rutina cargada/actualizada: $routine")
                    routine?.let {
                        vb.etRoutineName.setText(it.name)
                        triggersList.clear()
                        triggersList.addAll(it.triggers)
                        triggerAdapter.notifyDataSetChanged()
                        viewModel.updateActions(it.actions)
                        triggerAdapter.submitList(triggersList.toList())
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actions.collect { actions ->
                    Timber.d("Lista de acciones actualizada: $actions")
                    actionAdapter.submitList(actions)
                    actionAdapter.submitList(actions.toList())
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveResult.collect { result ->
                    when (result) {
                        is Resource.Success<Long> -> {
                            Timber.d("Rutina guardada con éxito, ID: ${'$'}{result.data}")
                            requireView().showSuccessSnackbar("Rutina guardada")
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        is Resource.Error -> {
                            Timber.e("Error al guardar rutina: ${'$'}{result.message}")
                            requireView().showErrorSnackbar(result.message ?: "Error desconocido")
                        }
                        Resource.Loading -> { /* Mostrar loader si lo deseas */ }
                    }
                }
            }
        }
    }

    private fun saveRoutine() {
        val name = vb.etRoutineName.text.toString().trim()
        if (validateForm(name)) {
            Timber.d("Guardando rutina con nombre: $name, triggers: ${'$'}triggersList, acciones: ${'$'}{viewModel.actions.value}")
            viewModel.saveRoutine(
                name = name,
                triggers = triggersList,
                actions = viewModel.actions.value
            )
        }
    }

    private fun validateForm(name: String): Boolean {
        var isValid = true
        if (name.isEmpty()) {
            vb.etRoutineName.error = "Nombre requerido"
            isValid = false
        }
        if (triggersList.isEmpty()) {
            requireView().showErrorSnackbar("Agrega al menos un trigger")
            isValid = false
        }
        if (viewModel.actions.value.isNullOrEmpty()) {
            requireView().showErrorSnackbar("Agrega al menos una acción")
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
            Timber.e("No se puede mostrar diálogo: Estado inválido del fragmento")
        }
    }
}