package com.example.rutinas.ui.routine.create

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.example.rutinas.viewmodel.RoutineViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    navController: NavController,
    routineViewModel: RoutineViewModel,
    onRoutineSaved: () -> Unit
) {
// Ejemplo de guardar:
// Button(onClick = { routineViewModel.saveRoutine(...); onRoutineSaved() }) { ... }

// Observa valores en el ViewModel
    val routineName by routineViewModel.routineName.collectAsState()
    val triggerType by routineViewModel.triggerType.collectAsState()
    val triggerData by routineViewModel.triggerData.collectAsState()
    val frequency by routineViewModel.selectedFrequency.collectAsState()
    val dayType by routineViewModel.selectedDayType.collectAsState()
    val dayValue by routineViewModel.selectedDayValue.collectAsState()
    val notify5MinBefore by routineViewModel.notify5MinBefore.collectAsState()
    val isActive by routineViewModel.isActive.collectAsState()
    val actions by routineViewModel.actions.collectAsState()
    val actionType by routineViewModel.actionType.collectAsState()
    val actionData by routineViewModel.actionData.collectAsState()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear/Editar Rutina") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Llamamos a la función de guardado
                    // Podríamos necesitar saber si es edición o creación,
                    // pero aquí asumimos que si la VM ya tiene un id, es edición
                    routineViewModel.saveRoutine(
                        routineId = null // O pasa el id real si mantuviste uno
                    )
                    navController.popBackStack()
                }
            ) {
                Text("Save")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nombre
            item {
                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineViewModel.setRoutineName(it) },
                    label = { Text("Routine Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Switch isActive
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Rutina activa? ")
                    Switch(
                        checked = isActive,
                        onCheckedChange = { routineViewModel.setIsActive(it) }
                    )
                }
            }

            // Trigger: Por ejemplo, si es TIME, pedimos la hora
            item {
                Text(
                    text = "Trigger: $triggerType",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                val calendar = Calendar.getInstance()
                if (triggerType == "TIME") {
                    Button(
                        onClick = {
                            val timeSetListener = TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val formatted = String.format("%02d:%02d", hourOfDay, minute)
                                    routineViewModel.updateTimeTrigger(formatted)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            )
                            timeSetListener.show()
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Select Time (actual: $triggerData)")
                    }
                }
                // O si "WIFI", entonces un OutlinedTextField para poner SSID
                if (triggerType == "WIFI") {
                    OutlinedTextField(
                        value = triggerData,
                        onValueChange = { routineViewModel.setTriggerData(it) },
                        label = { Text("SSID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Frecuencia
            item {
                Text(
                    text = "Frequency ($frequency)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                val frequencies = listOf("Once", "Daily", "Weekly", "Monthly")
                frequencies.forEach { freq ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (frequency == freq),
                            onClick = { routineViewModel.setSelectedFrequency(freq) }
                        )
                        Text(freq)
                    }
                }
            }

            // DayType / DayValue
            item {
                Text("Day Type: $dayType")
                val options = listOf("None", "Weekday", "Date")
                options.forEach { opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (dayType == opt),
                            onClick = { routineViewModel.setSelectedDayType(opt) }
                        )
                        Text(opt)
                    }
                }
                if (dayType != "None") {
                    OutlinedTextField(
                        value = dayValue,
                        onValueChange = { routineViewModel.setSelectedDayValue(it) },
                        label = { Text("Day Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Notificar 5 min antes
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notify 5 min before:")
                    Switch(
                        checked = notify5MinBefore,
                        onCheckedChange = { routineViewModel.setNotify5MinBefore(it) }
                    )
                }
            }

            // Sección para acción principal o multiples
            item {
                Text(
                    text = "Main Action: $actionType",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedTextField(
                    value = actionData,
                    onValueChange = { routineViewModel.setActionData(it) },
                    label = { Text("Action Data") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Ej. actionType = "ALARM", actionData = "Volumen alto"
                // Podrías hacer un ComboBox/radio para actionType
            }

            // Lista de “acciones” en orden
            item {
                Text(
                    text = "Actions (secundarias)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = { routineViewModel.addAction("Play Alarm") },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Action")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Action")
                }
            }

            itemsIndexed(actions) { index, action ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(text = "${index + 1}. $action")
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { routineViewModel.moveActionUp(index) }) {
                        Text("↑")
                    }
                    IconButton(onClick = { routineViewModel.moveActionDown(index) }) {
                        Text("↓")
                    }
                }
            }
        }
    }
}