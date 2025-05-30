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
import com.example.rutinas.databinding.FragmentPermissionsBinding
import timber.log.Timber

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

        // Add listener for the new button
        binding.btnRequestExactAlarmPermission.setOnClickListener {
            Timber.d("PermissionsFragment: 'Request Exact Alarm Permission' button clicked.")
            requestExactAlarmPermission()
        }
    }

    private fun checkAndDisplayPermissionStatus() {
        // --- Check SYSTEM_ALERT_WINDOW (Overlay Permission) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(requireContext())) {
                binding.tvOverlayPermissionStatus.text = "Estado: Concedido"
                binding.tvOverlayPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                binding.btnRequestOverlayPermission.visibility = View.GONE
            } else {
                binding.tvOverlayPermissionStatus.text = "Estado: Denegado"
                binding.tvOverlayPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                binding.btnRequestOverlayPermission.visibility = View.VISIBLE
            }
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

        // --- Check SCHEDULE_EXACT_ALARM Permission (for API 31+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // S is API 31
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                binding.tvExactAlarmPermissionStatus.text = "Estado: Concedido"
                binding.tvExactAlarmPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                binding.btnRequestExactAlarmPermission.visibility = View.GONE
            } else {
                binding.tvExactAlarmPermissionStatus.text = "Estado: Denegado"
                binding.tvExactAlarmPermissionStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                binding.btnRequestExactAlarmPermission.visibility = View.VISIBLE
            }
        } else {
            binding.tvExactAlarmPermissionStatus.text = "Estado: No necesario (Android < S)"
            binding.tvExactAlarmPermissionStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            binding.btnRequestExactAlarmPermission.visibility = View.GONE
        }
    }

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
    }

    private fun requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${requireContext().packageName}")
            )
            startActivity(intent)
        }
    }

    private fun requestNotificationListenerPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    // New function to request exact alarm permission
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // S is API 31
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = Uri.fromParts("package", requireContext().packageName, null) // Optional but good practice
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("PermissionsFragment: onDestroyView")
    }
}
