package com.example.rutinasapp.ui.routines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rutinasapp.data.RoutineEntity
import com.example.rutinasapp.databinding.FragmentRoutinesListBinding

class RoutinesListFragment : Fragment() {

    private var _binding: FragmentRoutinesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoutinesViewModel by viewModels()
    private lateinit var routinesAdapter: RoutinesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutinesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        routinesAdapter = RoutinesAdapter(
            listOf(),
            onItemClick = { routine ->
                // Manejar clic en la rutina
            },
            onSwitchChanged = { routine, isChecked ->
                // Manejar cambio de estado
            }
        )

        binding.routinesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.routinesRecyclerView.adapter = routinesAdapter

        // Observa los datos del ViewModel
        viewModel.routinesList.observe(viewLifecycleOwner) { routines ->
            routinesAdapter.updateData(routines)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}