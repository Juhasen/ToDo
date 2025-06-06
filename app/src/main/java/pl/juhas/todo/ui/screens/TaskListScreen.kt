package pl.juhas.todo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.juhas.todo.database.Category
import pl.juhas.todo.database.Task
import pl.juhas.todo.database.TaskStatus
import pl.juhas.todo.database.TaskWithAttachments
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskListScreen(
    tasks: List<TaskWithAttachments>,
    onTaskClick: (Task) -> Unit,
    onAddTask: () -> Unit,
    onTaskStatusChange: (Task) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj zadanie")
            }
        }
    ) { paddingValues ->

        Column {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TODO LIST",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tasks) { taskWithAttachments ->
                    TaskItem(
                        taskWithAttachments = taskWithAttachments,
                        onClick = { onTaskClick(taskWithAttachments.task) },
                        onStatusChange = { onTaskStatusChange(taskWithAttachments.task) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    taskWithAttachments: TaskWithAttachments,
    onClick: () -> Unit,
    onStatusChange: () -> Unit
) {
    val task = taskWithAttachments.task
    val hasAttachments = taskWithAttachments.attachments.isNotEmpty()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Kategoria zadania
                CategoryChip(category = task.category)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Opis zadania
            if (!task.description.isNullOrEmpty()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Dolna część karty z dodatkowymi informacjami
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Wyświetl czas wykonania, jeśli jest ustawiony
                    if (task.notifyAt != null) {
                        Text(
                            text = "Wykonaj do: ${dateFormat.format(Date(task.notifyAt))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ikona załączników
                    if (hasAttachments) {
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Załączniki",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Checkbox statusu
                    Checkbox(
                        checked = task.status == TaskStatus.DONE,
                        onCheckedChange = { onStatusChange() }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(category: Category) {
    val categoryColors = mapOf(
        Category.NORMAL to MaterialTheme.colorScheme.primary,
        Category.WORK to MaterialTheme.colorScheme.tertiary,
        Category.PERSONAL to MaterialTheme.colorScheme.secondary,
        Category.SHOPPING to MaterialTheme.colorScheme.error,
        Category.URGENT to MaterialTheme.colorScheme.error,
        Category.OTHER to MaterialTheme.colorScheme.outline
    )

    val chipColor = categoryColors[category] ?: MaterialTheme.colorScheme.primary

    SuggestionChip(
        onClick = { },
        label = { Text(text = category.name) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = chipColor.copy(alpha = 0.2f),
            labelColor = chipColor
        )
    )
}
