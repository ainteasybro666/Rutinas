package com.example.rutinas.ui.main.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rutinas.databinding.FragmentPermissionsBinding // Asegúrate de que se genere esta clase
import timber.log.Timber // Importar Timber

class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        Timber.d("PermissionsFragment: onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("PermissionsFragment: onViewCreated")

        setupButtonClickListeners()
    }

    override fun onResume() {
        super.onResume()
        Timber.d("PermissionsFragment: onResume. Checking permission statuses.")
        // Verificar el estado de los permisos cada vez que el fragmento se vuelve visible
        checkAndDisplayPermissionStatus()
    }

    private fun setupButtonClickListeners() {
        binding.btnRequestOverlayPermission.setOnClickListener {
            Timber.d("PermissionsFragment: 'Request Overlay Permission' button clicked.")
            requestOverlayPermission()
        }

        binding.btnRequestWriteSettingsPermission.setOnClickListener {
            Timber.d("PermissionsFragment: 'Request Write Settings Permission' button clicked.")
            requestWriteSettingsPermission()
        }

        binding.btnRequestNotificationListenerPermission.setOnClickListener {
            Timber.d("PermissionsFragment: 'Request Notification Listener Permission' button clicked.")
            requestNotificationListenerPermission()
        }
    }

    private fun checkAndDisplayPermissionStatus() {
        // --- Check SYSTEM_ALERT_WINDOW (Overlay Permission) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(requireContext())) {
                binding.tvOverlayPermissionStatus.text = "Estado: Concedido"
                binding.tvOverlayPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                binding.btnRequestOverlayPermission.visibility = View.GONE // Ocultar el botón si ya está concedido
            } else {
                binding.tvOverlayPermissionStatus.text = "Estado: Denegado"
                binding.tvOverlayPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                binding.btnRequestOverlayPermission.visibility = View.VISIBLE // Mostrar el botón si está denegado
            }
            // Si es una versión anterior a M, el permiso se otorga al instalar (si está en el manifiesto)
            // No necesitamos verificarlo aquí o mostrar el botón.
        } else {
            binding.tvOverlayPermissionStatus.text = "Estado: No necesario (Android < M)"
            binding.tvOverlayPermissionStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            binding.btnRequestOverlayPermission.visibility = View.GONE
        }


        // --- Check WRITE_SETTINGS Permission ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(requireContext())) {
                binding.tvWriteSettingsPermissionStatus.text = "Estado: Concedido"
                binding.tvWriteSettingsPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                binding.btnRequestWriteSettingsPermission.visibility = View.GONE
            } else {
                binding.tvWriteSettingsPermissionStatus.text = "Estado: Denegado"
                binding.tvWriteSettingsPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                binding.btnRequestWriteSettingsPermission.visibility = View.VISIBLE
            }
            // Si es una versión anterior a M, este permiso no es especial y se otorga con el grupo STORAGE/SYSTEM si se declara.
            // No necesitamos verificarlo aquí o mostrar el botón.
        } else {
            binding.tvWriteSettingsPermissionStatus.text = "Estado: No necesario (Android < M)"
            binding.tvWriteSettingsPermissionStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            binding.btnRequestWriteSettingsPermission.visibility = View.GONE
        }


        // --- Check Notification Listener Permission ---
        if (isNotificationListenerEnabled()) {
            binding.tvNotificationListenerPermissionStatus.text = "Estado: Concedido"
            binding.tvNotificationListenerPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            binding.btnRequestNotificationListenerPermission.visibility = View.GONE
        } else {
            binding.tvNotificationListenerPermissionStatus.text = "Estado: Denegado"
            binding.tvNotificationListenerPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            binding.btnRequestNotificationListenerPermission.visibility = View.VISIBLE
        }
    }

    // Function to check if notification listener is enabled
    private fun isNotificationListenerEnabled(): Boolean {
        val packageNames =
            Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        return packageNames != null && packageNames.contains(requireContext().packageName)
    }


    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent)
        }
        // En versiones anteriores a M, el permiso se otorga automáticamente con la declaración en el manifiesto.
        // No necesitamos hacer nada aquí.
    }

    private fun requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent)
        }
        // En versiones anteriores a M, este permiso se otorga con el grupo STORAGE/SYSTEM.
        // No necesitas hacer nada aquí.
    }

    private fun requestNotificationListenerPermission() {
        // Este intent lleva al usuario a la configuración de Acceso a Notificaciones
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpiar la referencia al binding
        Timber.d("PermissionsFragment: onDestroyView")
    }
}
