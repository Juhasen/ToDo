package pl.juhas.todo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import pl.juhas.todo.database.Category
import pl.juhas.todo.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val advanceTime by viewModel.notificationAdvanceTime.collectAsState()
    val hideDoneTasks by viewModel.hideDoneTasks.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()

    var minutesText by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    LaunchedEffect(advanceTime) {
        minutesText = (advanceTime / (60 * 1000)).toString()
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings(settingsManager)
    }

    // Efekt obsługujący wyświetlanie komunikatu o zapisaniu
    LaunchedEffect(saveMessage) {
        if (saveMessage.isNotEmpty()) {
            Toast.makeText(context, saveMessage, Toast.LENGTH_SHORT).show()
            saveMessage = "" // Resetujemy komunikat po wyświetleniu
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val minutes = minutesText.toIntOrNull()
                        if (minutes != null && minutes > 0) {
                            coroutineScope.launch {
                                viewModel.saveNotificationAdvanceTime(minutes, settingsManager)
                                saveMessage = "Ustawienia zapisane"
                            }
                        } else {
                            Toast.makeText(context, "Wprowadź prawidłową wartość (większą od 0)", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Zapisz")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sekcja powiadomień
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Powiadomienia",
                    style = MaterialTheme.typography.headlineSmall
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Powiadamiaj z wyprzedzeniem: ",
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(text = "minut")
                }

                Text(
                    text = "Aplikacja wyświetli powiadomienie określoną liczbę minut przed terminem wykonania zadania.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider()

            // Sekcja filtrów
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Domyślne filtry",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Opcja ukrywania zakończonych zadań
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ukryj zakończone zadania",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = hideDoneTasks,
                        onCheckedChange = {
                            viewModel.setHideDoneTasks(it, settingsManager)
                        }
                    )
                }

                // Nagłówek dla filtrowania kategorii
                Text(
                    text = "Domyślnie widoczne kategorie:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Lista kategorii z checkboxami
                Column {
                    Category.entries.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedCategories[category] ?: true,
                                onCheckedChange = { isChecked ->
                                    viewModel.setCategorySelected(category, isChecked, settingsManager)
                                }
                            )
                            Text(category.name)
                        }
                    }
                }

                // Przyciski zaznacz wszystkie/odznacz wszystkie
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            viewModel.setAllCategoriesSelected(true, settingsManager)
                            saveMessage = "Wszystkie kategorie zaznaczone"
                        }
                    ) {
                        Text("Zaznacz wszystkie")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            viewModel.setAllCategoriesSelected(false, settingsManager)
                            saveMessage = "Wszystkie kategorie odznaczone"
                        }
                    ) {
                        Text("Odznacz wszystkie")
                    }
                }
            }
        }
    }
}
