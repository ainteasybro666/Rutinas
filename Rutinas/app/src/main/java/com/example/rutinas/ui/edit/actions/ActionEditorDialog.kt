package com.example.rutinas.ui.edit.actions

import com.example.rutinas.data.model.Action

interface ActionEditorDialog {
    fun setOnActionUpdatedListener(listener: (Action) -> Unit)
}