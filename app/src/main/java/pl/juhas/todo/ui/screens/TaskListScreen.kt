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
import pl.juhas.todo.ui.composables.CategoryChip
import pl.juhas.todo.ui.composables.TaskItem
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