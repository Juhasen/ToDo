package pl.juhas.todo.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.juhas.todo.database.TaskStatus
import pl.juhas.todo.database.TaskWithAttachments
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Obliczanie czy zadanie jest przeterminowane
    val isOverdue = task.status != TaskStatus.DONE && task.finishAt < System.currentTimeMillis()

    // Obliczanie czasu pozostałego do terminu
    val timeLeftMillis = task.finishAt - System.currentTimeMillis()
    val timeLeftDays = timeLeftMillis / (1000 * 60 * 60 * 24)
    val timeLeftHours = (timeLeftMillis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.status == TaskStatus.DONE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.status == TaskStatus.DONE)
                            TextDecoration.LineThrough else TextDecoration.None
                    ),
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
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.status == TaskStatus.DONE)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Dolna część karty z dodatkowymi informacjami
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Informacja o terminie
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = if (isOverdue) MaterialTheme.colorScheme.error
                                  else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateFormat.format(Date(task.finishAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Pokaż czas pozostały tylko dla niezakończonych zadań
                    if (task.status != TaskStatus.DONE && !isOverdue && timeLeftDays < 7) {
                        Text(
                            text = when {
                                timeLeftDays > 0 -> "Pozostało: $timeLeftDays ${if (timeLeftDays == 1L) "dzień" else "dni"}"
                                timeLeftHours > 0 -> "Pozostało: $timeLeftHours ${if (timeLeftHours == 1L) "godzina" else "godzin"}"
                                else -> "Pozostało: < 1 godziny"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                        )
                    } else if (isOverdue) {
                        Text(
                            text = "Przeterminowane",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ikona załączników
                    if (hasAttachments) {
                        Badge(
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(text = "${taskWithAttachments.attachments.size}")
                        }
                        Icon(
                            imageVector = Icons.Default.Attachment,
                            contentDescription = "Załączniki",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    // Checkbox statusu z animacją
                    Checkbox(
                        checked = task.status == TaskStatus.DONE,
                        onCheckedChange = { onStatusChange() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = if (isOverdue) MaterialTheme.colorScheme.error
                                           else MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    }
}

