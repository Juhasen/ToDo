package pl.juhas.todo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import pl.juhas.todo.database.Attachment
import pl.juhas.todo.database.TaskStatus
import pl.juhas.todo.database.TaskWithAttachments
import pl.juhas.todo.ui.composables.ViewAttachmentItem
import pl.juhas.todo.utils.SettingsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskViewScreen(
    taskWithAttachments: TaskWithAttachments,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit
) {
    val task = taskWithAttachments.task
    val attachments = taskWithAttachments.attachments
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    // Pobieramy czas wyprzedzenia powiadomień z ustawień
    var notificationAdvanceTime by remember { mutableStateOf(30L * 60L * 1000L) } // domyślna wartość

    // Pobieramy rzeczywistą wartość z ustawień
    LaunchedEffect(key1 = Unit) {
        notificationAdvanceTime = settingsManager.notificationAdvanceTime.first()
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Więcej opcji")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edytuj") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Usuń") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
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
                .verticalScroll(rememberScrollState())
        ) {
            // Status zadania
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status: ${if (task.status == TaskStatus.DONE) "Zakończone" else "Do zrobienia"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(16.dp))
                if (task.status == TaskStatus.DONE) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kategoria
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kategoria:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                SuggestionChip(
                    onClick = { },
                    label = { Text(task.category.name) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data utworzenia
            Text(
                text = "Data utworzenia: ${dateFormat.format(Date(task.createdAt))}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Data wykonania (zawsze wyświetlana)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Data wykonania: ${dateFormat.format(Date(task.finishAt!!))}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Osobno wyświetlamy informację o powiadomieniu (tylko jeśli task.notify == true)
            if (task.notify) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Powiadomienie włączone na ${dateFormat.format(Date(task.finishAt - notificationAdvanceTime))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opis zadania
            Text(
                text = "Opis:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.description ?: "Brak opisu",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sekcja załączników
            if (attachments.isNotEmpty()) {
                Text(
                    text = "Załączniki (${attachments.size}):",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(attachments) { attachment ->
                        ViewAttachmentItem(
                            attachment = attachment,
                            onClick = { onOpenAttachment(attachment) }
                        )
                    }
                }
            }
        }
    }
}

