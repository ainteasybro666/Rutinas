package com.example.rutinas.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.showSuccessSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration)
        .setBackgroundTint(context.getColor(android.R.color.holo_green_dark))
        .show()
}

fun View.showErrorSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration)
        .setBackgroundTint(context.getColor(android.R.color.holo_red_dark))
        .show()
}

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}