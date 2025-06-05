package pl.juhas.todo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.juhas.todo.database.*
import pl.juhas.todo.ui.screens.TaskDetailScreen
import pl.juhas.todo.ui.screens.TaskListScreen
import pl.juhas.todo.ui.theme.TODOTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicjalizacja bazy danych
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo-database"
        ).build()

        setContent {
            TODOTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // Pobierz listę zadań z załącznikami
                var tasksWithAttachments by remember { mutableStateOf<List<TaskWithAttachments>>(emptyList()) }
                var currentTaskWithAttachments by remember { mutableStateOf<TaskWithAttachments?>(null) }

                // Pobierz zadania przy starcie
                LaunchedEffect(Unit) {
                    loadTasks { tasks ->
                        tasksWithAttachments = tasks
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "taskList"
                ) {
                    composable("taskList") {
                        TaskListScreen(
                            tasks = tasksWithAttachments,
                            onTaskClick = { task ->
                                currentTaskWithAttachments = tasksWithAttachments.find { it.task.id == task.id }
                                navController.navigate("taskDetail")
                            },
                            onAddTask = {
                                currentTaskWithAttachments = null
                                navController.navigate("taskDetail")
                            },
                            onTaskStatusChange = { task ->
                                val newStatus = if (task.status == TaskStatus.TODO) TaskStatus.DONE else TaskStatus.TODO
                                val updatedTask = task.copy(status = newStatus)

                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        database.taskDao().updateTask(updatedTask)
                                    }

                                    // Odśwież listę zadań
                                    loadTasks { tasks ->
                                        tasksWithAttachments = tasks
                                    }
                                }
                            }
                        )
                    }

                    composable("taskDetail") {
                        TaskDetailScreen(
                            taskWithAttachments = currentTaskWithAttachments,
                            onSave = { updatedTask ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (updatedTask.id == 0) {
                                            // Nowe zadanie
                                            val newTaskId = database.taskDao().insertTask(
                                                updatedTask.copy(id = 0)
                                            ).toInt()

                                            // Aktualizacja ID załączników, jeśli są
                                            currentTaskWithAttachments?.attachments?.forEach { attachment ->
                                                database.attachmentDao().insertAttachment(
                                                    attachment.copy(taskId = newTaskId)
                                                )
                                            }
                                        } else {
                                            // Aktualizacja istniejącego zadania
                                            database.taskDao().updateTask(updatedTask)
                                        }
                                    }

                                    // Odśwież listę zadań
                                    loadTasks { tasks ->
                                        tasksWithAttachments = tasks
                                    }

                                    // Wróć do listy zadań
                                    navController.navigateUp()
                                }
                            },
                            onCancel = {
                                navController.navigateUp()
                            },
                            onAddAttachment = { attachment ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (currentTaskWithAttachments?.task?.id != 0) {
                                            database.attachmentDao().insertAttachment(attachment)

                                            // Odśwież zadanie z załącznikami
                                            currentTaskWithAttachments?.task?.id?.let { taskId ->
                                                val refreshedTask = database.attachmentDao().getTaskWithAttachments(taskId)
                                                withContext(Dispatchers.Main) {
                                                    currentTaskWithAttachments = refreshedTask
                                                }
                                            }
                                        } else {
                                            // Tymczasowo dodaj załącznik do zadania, które jeszcze nie ma ID
                                            val updatedAttachments = currentTaskWithAttachments?.attachments?.toMutableList() ?: mutableListOf()
                                            updatedAttachments.add(attachment)

                                            withContext(Dispatchers.Main) {
                                                currentTaskWithAttachments = TaskWithAttachments(
                                                    task = currentTaskWithAttachments?.task ?: Task(
                                                        id = 0,
                                                        title = "",
                                                        description = null,
                                                        createdAt = System.currentTimeMillis()
                                                    ),
                                                    attachments = updatedAttachments
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            onRemoveAttachment = { attachment ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (attachment.id != 0) {
                                            database.attachmentDao().deleteAttachment(attachment)
                                        }

                                        // Odśwież zadanie z załącznikami
                                        if (currentTaskWithAttachments?.task?.id != 0) {
                                            currentTaskWithAttachments?.task?.id?.let { taskId ->
                                                val refreshedTask = database.attachmentDao().getTaskWithAttachments(taskId)
                                                withContext(Dispatchers.Main) {
                                                    currentTaskWithAttachments = refreshedTask
                                                }
                                            }
                                        } else {
                                            // Usuń załącznik z tymczasowego zadania
                                            val updatedAttachments = currentTaskWithAttachments?.attachments?.toMutableList() ?: mutableListOf()
                                            updatedAttachments.remove(attachment)

                                            withContext(Dispatchers.Main) {
                                                currentTaskWithAttachments = currentTaskWithAttachments?.copy(
                                                    attachments = updatedAttachments
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            onOpenAttachment = { attachment ->
                                try {
                                    val uri = Uri.parse(attachment.filePath)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, attachment.mimeType)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Nie można otworzyć pliku: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun loadTasks(onComplete: (List<TaskWithAttachments>) -> Unit) {
        lifecycleScope.launch {
            val tasks = withContext(Dispatchers.IO) {
                database.attachmentDao().getAllTasksWithAttachments()
            }
            onComplete(tasks)
        }
    }
}
