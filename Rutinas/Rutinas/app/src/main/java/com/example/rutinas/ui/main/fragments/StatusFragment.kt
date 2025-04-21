// StatusFragment.kt
package com.example.rutinas.ui.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.rutinas.databinding.FragmentStatusBinding
import com.example.rutinas.ui.common.BaseFragment

class StatusFragment : BaseFragment() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding {
        return FragmentStatusBinding.inflate(inflater, container, false)
    }

    // Propiedad tipada para facilitar el uso del binding concreto
    private val vb: FragmentStatusBinding
        get() = binding as FragmentStatusBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vb.tvStatusTitle.text = "Estado"
    }

    fun onPermissionGranted() {
        // Lógica para manejar permisos concedidos
    }
}