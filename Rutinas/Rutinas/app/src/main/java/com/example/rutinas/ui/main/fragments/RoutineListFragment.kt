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
        adapter = RoutineAdapter { routine, isChecked ->
            viewModel.toggleRoutineStatus(routine.id, isChecked)
        }
        vb.rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        vb.rvRoutines.adapter = adapter
    }

    private fun observeData() {
        viewModel.routines.observe(viewLifecycleOwner) { routines ->
            adapter.submitList(routines)
        }
    }

    fun onPermissionGranted() {
        // Lógica para manejar permisos concedidos
    }
}