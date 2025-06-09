package pl.juhas.todo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
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

    // Obliczanie czy zadanie jest przeterminowane
    val isOverdue = task.status != TaskStatus.DONE && task.finishAt < System.currentTimeMillis()

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
                .verticalScroll(rememberScrollState())
        ) {
            // Karta główna z informacjami o zadaniu
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Status zadania
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Status: ",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        AssistChip(
                            onClick = { },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when {
                                    task.status == TaskStatus.DONE -> MaterialTheme.colorScheme.primaryContainer
                                    isOverdue -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                labelColor = when {
                                    task.status == TaskStatus.DONE -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            ),
                            leadingIcon = when {
                                task.status == TaskStatus.DONE -> {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                isOverdue -> {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                                else -> null
                            },
                            label = {
                                Text(if (task.status == TaskStatus.DONE) "Zakończone" else "Do zrobienia")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Kategoria
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kategoria:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        SuggestionChip(
                            onClick = { },
                            label = { Text(task.category.name) }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Daty i powiadomienia
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Data utworzenia
                        InfoRow(
                            icon = Icons.Default.DateRange,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "Data utworzenia:",
                            value = dateFormat.format(Date(task.createdAt))
                        )

                        // Data wykonania
                        InfoRow(
                            icon = Icons.Default.Event,
                            iconTint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            label = "Data wykonania:",
                            value = dateFormat.format(Date(task.finishAt!!)),
                            extraInfo = if (isOverdue) "Przeterminowane" else null,
                            extraInfoColor = MaterialTheme.colorScheme.error
                        )

                        // Powiadomienie (tylko jeśli włączone)
                        if (task.notify) {
                            InfoRow(
                                icon = Icons.Default.Notifications,
                                iconTint = MaterialTheme.colorScheme.secondary,
                                label = "Powiadomienie:",
                                value = dateFormat.format(Date(task.finishAt - notificationAdvanceTime)),
                                extraInfo = "${notificationAdvanceTime / (60 * 1000)} minut przed terminem",
                                extraInfoColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Sekcja opisu
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Opis",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (task.description.isNullOrBlank()) {
                        Text(
                            text = "Brak opisu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Sekcja załączników
            if (attachments.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Załączniki (${attachments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Pomocniczy komponent do wyświetlania informacji z ikoną
@Composable
private fun InfoRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    extraInfo: String? = null,
    extraInfoColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Dodatkowa informacja, jeśli istnieje
        if (extraInfo != null) {
            Text(
                text = extraInfo,
                style = MaterialTheme.typography.labelSmall,
                color = extraInfoColor,
                modifier = Modifier.padding(start = 28.dp, top = 2.dp)
            )
        }
    }
}
