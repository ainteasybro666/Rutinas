package com.example.rutinasapp.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.rutinasapp.ui.routines.RoutinesListFragment

class MainPageAdapter(fragmentActivity: FragmentManager) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3  // Tenemos 3 pestañas

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RoutinesListFragment()   // Pestaña principal con las rutinas
            1 -> Fragment()              // Pestaña “placeholder”
            2 -> Fragment()              // Pestaña “placeholder”
            else -> Fragment()
        }
    }
}