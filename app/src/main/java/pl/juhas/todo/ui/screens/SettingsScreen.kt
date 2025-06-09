package pl.juhas.todo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
    var minutesText by remember { mutableStateOf("") }

    LaunchedEffect(advanceTime) {
        minutesText = (advanceTime / (60 * 1000)).toString()
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings(settingsManager)
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
                                Toast.makeText(context, "Ustawienia zapisane", Toast.LENGTH_SHORT).show()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Powiadomienia",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

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
    }
}
