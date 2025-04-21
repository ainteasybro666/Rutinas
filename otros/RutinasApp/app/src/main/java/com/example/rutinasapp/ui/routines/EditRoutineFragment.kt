package com.example.rutinasapp.ui.routines

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.example.rutinasapp.data.Action
import com.example.rutinasapp.data.Routine
import com.example.rutinasapp.databinding.FragmentEditRoutineBinding
import com.google.android.material.snackbar.Snackbar

class EditRoutineFragment : DialogFragment() {

    private var _binding: FragmentEditRoutineBinding? = null
    private val binding get() = _binding!!

    private var routine: Routine? = null
    private lateinit var actionsAdapter: ActionsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentEditRoutineBinding.inflate(LayoutInflater.from(context))

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setCancelable(true)

        // Recuperar rutina si viene como argumento
        routine = arguments?.getParcelable<BundledRoutine>("routine")?.toRoutine()

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { d ->
            d.window?.setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Cargar datos iniciales
        binding.etRoutineName.setText(routine?.name ?: "")

        // Configurar RecyclerView de acciones
        val existingActions = routine?.actions ?: mutableListOf()
        actionsAdapter = ActionsAdapter(existingActions)
        binding.rvActions.adapter = actionsAdapter

        // Botón Agregar acción
        binding.btnAddAction.setOnClickListener {
            existingActions.add(Action("Nueva acción", existingActions.size + 1))
            actionsAdapter.notifyItemInserted(existingActions.lastIndex)
        }

        // Botón Guardar
        binding.btnSaveRoutine.setOnClickListener {
            val updatedName = binding.etRoutineName.text.toString()
            routine?.name = updatedName
            // Podrías actualizar triggers si tuvieras inputs
            // Guardar/actualizar la rutina en tu base de datos, etc.

            dismiss()  // Cierra el diálogo
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                "Rutina guardada: $updatedName",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Para pasar una rutina (opcional) al fragmento
    companion object {
        fun newInstance(routine: Routine? = null): EditRoutineFragment {
            val fragment = EditRoutineFragment()
            routine?.let {
                val bundle = Bundle()
                bundle.putParcelable("routine", BundledRoutine.fromRoutine(it))
                fragment.arguments = bundle
            }
            return fragment
        }
    }
}