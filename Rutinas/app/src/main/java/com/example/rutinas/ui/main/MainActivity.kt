package com.example.rutinas.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.rutinas.R // Asegúrate de que R apunte a tu archivo de recursos
import com.example.rutinas.databinding.ActivityMainBinding // Asegúrate de que ActivityMainBinding es correcto
import com.example.rutinas.ui.common.BaseActivity
import com.example.rutinas.ui.main.fragments.RoutineListFragmentDirections // Asegúrate de que esta importación es correcta
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Simplificado: El listener solo maneja la visibilidad de BottomNavigationView y FAB
    private val destinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            Timber.d("MainActivity: Destination changed to ${destination.label}")

            // Controlar la visibilidad de la BottomNavigationView y FAB
            when (destination.id) {
                R.id.routineListFragment, R.id.statusFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.fabAddRoutine.visibility = if (destination.id == R.id.routineListFragment) View.VISIBLE else View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.fabAddRoutine.visibility = View.GONE
                }
            }

            // La Toolbar de la Activity siempre será visible con esta estrategia.
            Timber.d("MainActivity: Toolbar visibility not changed for destination ${destination.label}")
        }

    override fun inflateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupViews() {
        setupNavigation()
        setupFab()

        // Configurar la Toolbar de la Activity como ActionBar Y vincular con NavController UNA VEZ
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        Timber.d("MainActivity: Initial setupActionBarWithNavController called in setupViews")
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.routineListFragment,
                R.id.statusFragment
            )
        )

        // Agregar el listener DESPUÉS de configurar la navegación inicial
        navController.addOnDestinationChangedListener(destinationChangedListener)
        Timber.d("MainActivity: destinationChangedListener added")
    }

    private fun setupFab() {
        binding.fabAddRoutine.setOnClickListener {
            if (navController.currentDestination?.id == R.id.routineListFragment) {
                Timber.d("MainActivity: FAB clicked, navigating to RoutineEditFragment (new routine)")
                val action =
                    RoutineListFragmentDirections.actionRoutineListFragmentToRoutineEditFragment(
                        routineUuid = null // Para crear una nueva rutina
                    )
                navController.navigate(action)
            } else {
                Toast.makeText(this, "Acción no disponible en este contexto", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflar el menú solo si estamos en routineListFragment
        // Y si la Toolbar de la Activity está visible (lo estará siempre con esta estrategia)
        return if (navController.currentDestination?.id == R.id.routineListFragment) {
            Timber.d("MainActivity: Inflating Options Menu for RoutineListFragment")
            menuInflater.inflate(R.menu.toolbar_menu, menu) // Asegúrate de que toolbar_menu existe
            true
        } else {
            Timber.d("MainActivity: Not inflating Options Menu for destination ${navController.currentDestination?.label}")
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> { // Asegúrate de que action_settings existe en toolbar_menu
                Timber.d("MainActivity: Settings menu item clicked. Manually navigating to SettingsFragment.")
                val action = RoutineListFragmentDirections.actionRoutineListFragmentToSettingsFragment() // Asegúrate de que esta acción existe
                navController.navigate(action)
                true
            }
            // Dejar que NavigationUI maneje la navegación hacia arriba para todos los fragmentos
            // donde la Toolbar de la Activity es la ActionBar.
            android.R.id.home -> {
                Timber.d("MainActivity: android.R.id.home clicked. Letting NavigationUI handle it via Activity Toolbar.")
                NavigationUI.navigateUp(navController, appBarConfiguration) || super.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setupViews() ya se llama en BaseActivity.onCreate()
    }

    override fun onSupportNavigateUp(): Boolean {
        // NavigationUI.navigateUp intentará navegar hacia arriba usando la ActionBar configurada (la de la Activity).
        Timber.d("MainActivity: onSupportNavigateUp called. Letting NavigationUI handle it via Activity Toolbar.")
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity: onDestroy, removing destinationChangedListener")
        navController.removeOnDestinationChangedListener(destinationChangedListener)
    }
}
