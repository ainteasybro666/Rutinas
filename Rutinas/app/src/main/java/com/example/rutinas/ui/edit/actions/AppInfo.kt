package com.example.rutinas.ui.edit.actions

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    var selected: Boolean = true // Todas seleccionadas por defecto
)