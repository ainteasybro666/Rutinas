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
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint // Asegurarse que tiene la anotación
class EditReadNotificationsActionDialogFragment(listener: ActionDialogListener) :
    BaseEditActionDialogFragment<FragmentEditReadNotificationsActionBinding>(listener) {

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
    ): FragmentEditReadNotificationsActionBinding { // Cambiar tipo de retorno
        return FragmentEditReadNotificationsActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("EditReadNotificationsActionDialogFragment: onViewCreated() llamado")
        setupRecyclerView()
        loadActionData(actionToEdit) // Usar actionToEdit
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

    override fun loadActionData(action: Action?) { // Implementar método abstracto
        Timber.d("EditReadNotificationsActionDialogFragment: loadActionData() llamado")
        try {
            val newAppList = getAppList().toMutableList()
            action?.data?.let { dataWrapper -> // Usar action?.data y actionToLoad si usas el parámetro
                dataWrapper.data.let { data -> // Usar dataWrapper
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

    override fun saveActionData(): Action { // Implementar método abstracto
        Timber.d("EditReadNotificationsActionDialogFragment: saveActionData() llamado")
        try {
            val selectedApps = appList.filter { it.selected }.map { it.packageName }
            val updatedAction = actionToEdit.copy( // Usar actionToEdit
                actionType = ActionType.READ_NOTIFICATIONS,
                data = DataWrapper(
                    mapOf(
                        "selectedApps" to selectedApps
                    ) as MutableMap<String, Any> // Añadir cast
                )
            )
            return updatedAction
            Timber.d("EditReadNotificationsActionDialogFragment: Acción actualizada y notificada")
        } catch (e: Exception) {
            Timber.e("EditReadNotificationsActionDialogFragment: Error al guardar acción - ${e.message}")
            throw e // Relanzar la excepción
        }
    }

    // Implementar onSaveAction
//    override fun onSaveAction() {
//        Timber.d("EditReadNotificationsActionDialogFragment: onSaveAction() llamado")
//        try {
//            val updatedAction = saveActionData()
//            notifyActionUpdated(updatedAction)
//            Timber.d("EditReadNotificationsActionDialogFragment: Action updated and notified")
//            dismiss()
//        } catch (e: Exception) {
//            Timber.e("EditReadNotificationsActionDialogFragment: Error saving action - ${e.message}")
//            showErrorDialog("Error al guardar la acción: ${e.message}")
//        }
//    }
}
