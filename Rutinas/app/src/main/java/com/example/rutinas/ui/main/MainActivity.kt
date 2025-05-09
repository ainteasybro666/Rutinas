package com.example.rutinas.ui.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.rutinas.R
import com.example.rutinas.databinding.ActivityMainBinding
import com.example.rutinas.ui.common.BaseActivity
import com.example.rutinas.ui.main.fragments.RoutineListFragmentDirections
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var navController: NavController

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupViews() {
        setupNavigation()
        binding.bottomNavigation.visibility = View.VISIBLE
        setupFab()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
    }

    private fun setupFab() {
        binding.fabAddRoutine.setOnClickListener {
            // Solo navegar a la edición desde el fragmento de rutinas ("Rutinas")
            if (navController.currentDestination?.id == R.id.routineListFragment) {
                // Navegar a RoutineEditFragment pasando null como UUID para crear una nueva rutina
                // Usamos la clase Directions generada por el Navigation Component
                val action = RoutineListFragmentDirections.actionRoutineListFragmentToRoutineEditFragment(routineUuid = null as String?)
                navController.navigate(action)
            } else {
                Toast.makeText(this, "Acción no disponible en este contexto", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // BaseActivity se encarga de inflar el binding y llamar a setupViews()
    }
}