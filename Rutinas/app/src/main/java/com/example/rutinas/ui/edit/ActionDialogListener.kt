package com.example.rutinas.ui.edit

import com.example.rutinas.data.model.Action

interface ActionDialogListener {
    fun onActionUpdated(updatedAction: Action)
}
