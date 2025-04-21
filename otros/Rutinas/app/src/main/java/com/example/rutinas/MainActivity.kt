package com.example.rutinas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.example.rutinas.ui.theme.RutinasTheme
import com.example.rutinas.viewmodel.RoutineViewModel
import com.example.rutinas.ui.routine.create.CreateRoutineScreen
import com.example.rutinas.ui.routine.list.RoutineListScreen
import androidx.compose.ui.Modifier

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RutinasTheme {
                val navController = rememberNavController()
                val viewModel: RoutineViewModel by viewModels()

                Scaffold { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "listScreen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // LIST screen
                        composable("listScreen") {
                            RoutineListScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }

                        // CREATE screen
                        composable("createScreen") {
                            CreateRoutineScreen(
                                navController = navController,
                                routineViewModel = viewModel,
                                onRoutineSaved = {
                                    // Ejemplo: volver atrás cuando se guarde
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
sealed class Screen(val route: String) {
    object ListScreen : Screen("listScreen")
    object CreateScreen : Screen("createScreen")
}