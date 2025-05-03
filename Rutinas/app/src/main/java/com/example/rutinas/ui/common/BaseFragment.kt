package com.example.rutinas.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    private var _binding: ViewBinding? = null
    protected val binding: ViewBinding
        get() = _binding!!

    /**
     * Méto-do abstracto para inflar el binding.
     * Cada fragmento deberá implementar este méto-do y devolver su instancia de ViewBinding.
     */
    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): ViewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


