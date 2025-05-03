package com.example.rutinas.ui.edit.actions

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.example.rutinas.data.model.Action
import com.example.rutinas.data.model.ActionType
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.databinding.FragmentEditReadNotificationsActionBinding
import com.example.rutinas.ui.edit.ActionDialogListener
import com.example.rutinas.ui.edit.RoutineEditFragment
import com.example.rutinas.ui.main.adapter.AppInfoAdapter
import timber.log.Timber

class EditReadNotificationsActionDialogFragment(listener: ActionDialogListener) :
    BaseEditActionDialogFragment(listener) {
    private var _binding: FragmentEditReadNotificationsActionBinding? = null
    private val binding get() = _binding!!
    private lateinit var appAdapter: AppInfoAdapter
    private var appList: MutableList<AppInfo> = mutableListOf()

    companion object {
        fun newInstance(
            action: Action,
            listener: RoutineEditFragment
        ): EditReadNotificationsActionDialogFragment {
            return EditReadNotificationsActionDialogFragment(listener).apply {
                arguments = newBundle(action)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding {
        _binding = FragmentEditReadNotificationsActionBinding.inflate(inflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditReadNotificationsActionDialogFragment: onViewCreated() llamado")
        setupRecyclerView()
        loadActionData()
    }

    private fun setupRecyclerView() {
        Timber.d("EditReadNotificationsActionDialogFragment: setupRecyclerView() llamado")
        appAdapter = AppInfoAdapter(appList)
        binding.rvAppList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = appAdapter
        }
    }

    private fun getAppList(): List<AppInfo> {
        Timber.d("EditReadNotificationsActionDialogFragment: getAppList() llamado")
        val packageManager = requireContext().packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return packages.map { app ->
            AppInfo(
                appName = packageManager.getApplicationLabel(app).toString(),
                packageName = app.packageName,
                icon = packageManager.getApplicationIcon(app),
            )
        }
    }

    fun loadActionData() {
        Timber.d("EditReadNotificationsActionDialogFragment: loadActionData() llamado")
        try {
            val newAppList = getAppList().toMutableList()
            action.data?.let { dataWrapper ->
                dataWrapper.data.let { data ->
                    val selectedApps = data["selectedApps"] as? List<String> ?: emptyList()
                    newAppList.forEach { appInfo ->
                        appInfo.selected = selectedApps.contains(appInfo.packageName)
                    }
                }
            }
            appList.clear()
            appList.addAll(newAppList)
            appAdapter.updateData(appList)
        } catch (e: Exception) {
            Timber.e("EditReadNotificationsActionDialogFragment: Error al cargar datos - ${e.message}")
        }
    }

    override fun saveAction() {
        Timber.d("EditReadNotificationsActionDialogFragment: saveAction() llamado")
        try {
            val selectedApps = appList.filter { it.selected }.map { it.packageName }
            val updatedAction = action.copy(
                actionType = ActionType.READ_NOTIFICATIONS,
                data = DataWrapper(
                    mapOf(
                        "selectedApps" to selectedApps
                    )
                )
            )
            notifyActionUpdated(updatedAction)
            Timber.d("EditReadNotificationsActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditReadNotificationsActionDialogFragment: Error al guardar acción - ${e.message}")
        }
    }

    override fun onDestroyView() {
        Timber.d("EditReadNotificationsActionDialogFragment: onDestroyView() llamado")
        super.onDestroyView()
        _binding = null
    }
}
