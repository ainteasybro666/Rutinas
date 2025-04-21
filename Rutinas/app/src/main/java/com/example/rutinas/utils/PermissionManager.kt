package com.example.rutinas.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber


class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _permissionStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionStatus: StateFlow<Map<String, Boolean>> = _permissionStatus

    fun checkPermissions(vararg permissions: String): Map<String, Boolean> {
        Timber.d("Verificando permisos: ${permissions.joinToString()}")
        return permissions.associateWith { permission ->
            val result = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            Timber.d("Permiso $permission: ${if (result) "Concedido" else "Denegado"}")
            result
        }.also {
            _permissionStatus.value = it
        }
    }

    fun onPermissionsResult(permissions: Array<out String>, grantResults: IntArray) {
        Timber.d("Procesando resultados de permisos")
        val results = permissions.zip(grantResults.toList()).associate { (perm, result) ->
            val granted = result == PackageManager.PERMISSION_GRANTED
            Timber.d("Resultado permiso $perm: ${if (granted) "Concedido" else "Denegado"}")
            perm to granted
        }
        _permissionStatus.value = _permissionStatus.value + results
    }
}