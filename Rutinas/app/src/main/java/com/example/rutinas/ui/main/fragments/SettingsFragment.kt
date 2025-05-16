package com.example.rutinas.ui.main.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController // Importar findNavController
import com.example.rutinas.R
import com.example.rutinas.databinding.FragmentSettingsBinding
import timber.log.Timber

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        Timber.d("SettingsFragment: onCreateView")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("SettingsFragment: onViewCreated")

        // ** Configurar el listener para la opción de Permisos **
        binding.tvPermissionsSetting.setOnClickListener {
            Timber.d("SettingsFragment: 'Permisos de la Aplicación' clicked. Navigating to PermissionsFragment.")
            // Navegar al fragmento de permisos.
            // Asegúrate de que la acción 'action_settingsFragment_to_permissionsFragment'
            // esté definida en tu nav_graph.xml saliendo de settingsFragment
            val action = SettingsFragmentDirections.actionSettingsFragmentToPermissionsFragment() // Usar Safe Args
            findNavController().navigate(action)
        }

        // Puedes añadir listeners para otros TextViews de configuración aquí
        // binding.tvAnotherSetting.setOnClickListener { ... }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpiar la referencia al binding
        Timber.d("SettingsFragment: onDestroyView")
    }
}
