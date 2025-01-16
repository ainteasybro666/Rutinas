package com.example.rutinas.ui.routine.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rutinas.MainActivity
import com.example.rutinas.Screen
import com.example.rutinas.data.model.Routine
import com.example.rutinas.viewmodel.RoutineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    navController: NavController,
    viewModel: RoutineViewModel
) {
// Ejemplo para navegar a createScreen
// Button(onClick = { navController.navigate("createScreen") }) { ... }

    val routines by viewModel.routines.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Rutinas") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Limpia estados en la VM para "nueva rutina"
                viewModel.setRoutineName("")
                viewModel.setTriggerType("TIME")
                viewModel.setTriggerData("")
                viewModel.setSelectedFrequency("Once")
                viewModel.setSelectedDayType("None")
                viewModel.setSelectedDayValue("")
                viewModel.setNotify5MinBefore(false)
                viewModel.setIsActive(true)
                viewModel.setActionType("ALARM")
                viewModel.setActionData("")
                // También limpia la lista de acciones
                // Por simplicidad podemos reasignar la StateFlow de la VM en un setter adicional
                // o un viewModel.clearActions().
                // Llama a la pantalla de creación:
                navController.navigate(Screen.ListScreen.route)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Routine")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(routines) { routine ->
                Text(
                    text = routine.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Cargar en la VM los datos de esta rutina
                            viewModel.setRoutineName(routine.name)
                            viewModel.setTriggerType(routine.triggerType)
                            viewModel.setTriggerData(routine.triggerData)
                            viewModel.setSelectedFrequency(routine.frequency)
                            viewModel.setSelectedDayType(routine.dayType)
                            viewModel.setSelectedDayValue(routine.dayValue)
                            viewModel.setNotify5MinBefore(routine.notify5MinBefore)
                            viewModel.setIsActive(routine.isActive)
                            viewModel.setActionType(routine.actionType)
                            viewModel.setActionData(routine.actionData)
                            // Cargar la lista de acciones
                            // Podrías crear un viewModel.setActions(routine.actions)
                            // y mandar la lista allí.
                            // Después navegas a la pantalla de creación/edición
                            navController.navigate(Screen.CreateScreen.route)
                        }
                        .padding(16.dp)
                )
            }
        }
    }
}