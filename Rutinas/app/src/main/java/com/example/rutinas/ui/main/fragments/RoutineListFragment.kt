// RoutineListFragment.kt
package com.example.rutinas.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.example.rutinas.databinding.FragmentRoutineListBinding
import com.example.rutinas.ui.common.BaseFragment
import com.example.rutinas.ui.main.adapter.RoutineAdapter
import com.example.rutinas.ui.viewmodel.RoutineListViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.app.AlertDialog // Import AlertDialog
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity

// Importar NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.rutinas.domain.Routine
import timber.log.Timber

@AndroidEntryPoint
class RoutineListFragment : BaseFragment() {

    private val viewModel: RoutineListViewModel by viewModels()
    private lateinit var adapter: RoutineAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding {
        return FragmentRoutineListBinding.inflate(inflater, container, false)
    }

    // Propiedad tipada para facilitar el uso del binding concreto
    private val vb: FragmentRoutineListBinding
        get() = binding as FragmentRoutineListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = RoutineAdapter(
            onSwitchChanged = { routine, isChecked ->
                // Manejar el cambio del switch (ya implementado)
                viewModel.toggleRoutineStatus(routine.id, isChecked)
            },
            onRoutineClicked = { routine ->
                // Manejar el clic normal en el item de la rutina (edición)
                navigateToEditRoutine(routine.uuid)
            },
            onRoutineLongClicked = { routine ->
                // NEW: Handle long click (show delete option)
                showDeleteRoutineDialog(routine)
            }
        )
        vb.rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        vb.rvRoutines.adapter = adapter
    }

    private fun observeData() {
        viewModel.routines.observe(viewLifecycleOwner) { routines ->
            adapter.submitList(routines)
        }
    }

    // Función para navegar al fragmento de edición
    fun navigateToEditRoutine(routineUuid: String?) {
        Timber.d("RoutineListFragment: navigateToEditRoutine called for UUID: $routineUuid")
        // Asegúrate de que la acción de navegación está definida en tu nav_graph.xml
        // y que acepta un argumento 'routine_uuid' de tipo String.

        // ** >>>>> INICIO: Desvincular la ActionBar de la Activity de NavigationUI <<<<< **
        // Obtener una referencia a la Activity y su ActionBar si está configurada
        val activity = requireActivity()
        if (activity is AppCompatActivity) {
            val supportActionBar = activity.supportActionBar
            if (supportActionBar != null) {
                // Deshabilitar la flecha "Up" y potencialmente otras interacciones de NavigationUI
                // al desvincular la ActionBar.
                // Esto evita que NavigationUI intente actualizar la ActionBar durante la navegación.
                NavigationUI.setupActionBarWithNavController(activity, findNavController(), null) // Pasamos null para desvincular
                // Opcional: Ocultar la Toolbar de la Activity aquí también para ser más explícito
                //activity.findViewById<Toolbar>(R.id.toolbar)?.visibility = View.GONE // Si la Toolbar de la Activity tiene este ID
            }
        }
        // ** >>>>> FIN: Desvincular la ActionBar de la Activity de NavigationUI <<<<< **


        // Iniciar la navegación
        val action = RoutineListFragmentDirections.actionRoutineListFragmentToRoutineEditFragment(routineUuid = routineUuid)
        findNavController().navigate(action)
    }

    // NEW: Function to show delete confirmation dialog
    private fun showDeleteRoutineDialog(routine: Routine) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Rutina")
            .setMessage("¿Estás seguro de que quieres eliminar la rutina '${routine.name}'?")
            .setPositiveButton("Eliminar") { dialog, which ->
                // Call ViewModel to delete the routine
                viewModel.deleteRoutine(routine) // NEW: Call the delete function in ViewModel
            }
            .setNegativeButton("Cancelar", null) // Dismiss the dialog on cancel
            .setIcon(android.R.drawable.ic_dialog_alert) // Optional: Add an alert icon
            .show()
    }


    fun onPermissionGranted() {
        // Lógica para manejar permisos concedidos
    }
}
