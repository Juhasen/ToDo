package pl.juhas.todo

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.juhas.todo.database.AppDatabase
import pl.juhas.todo.database.Task
import pl.juhas.todo.database.TaskStatus
import pl.juhas.todo.database.TaskWithAttachments
import pl.juhas.todo.ui.screens.SettingsScreen
import pl.juhas.todo.ui.screens.TaskEditScreen
import pl.juhas.todo.ui.screens.TaskListScreen
import pl.juhas.todo.ui.screens.TaskViewScreen
import pl.juhas.todo.ui.theme.TODOTheme
import pl.juhas.todo.utils.AttachmentHelper
import pl.juhas.todo.utils.SettingsManager
import android.Manifest
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var attachmentHelper: AttachmentHelper
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicjalizacja bazy danych
        database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "todo-database"
            ).fallbackToDestructiveMigration(false).build()

        // Inicjalizacja pomocnika załączników
        attachmentHelper = AttachmentHelper(this)


        settingsManager = SettingsManager(this)

        // Sprawdzenie i żądanie uprawnień do powiadomień
        checkNotificationPermission()

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

                        // Sprawdź, czy aplikacja została uruchomiona z powiadomienia
                        val taskIdFromNotification = intent.getIntExtra("TASK_ID", -1)
                        if (taskIdFromNotification > 0) {
                            // Znajdź zadanie o podanym identyfikatorze
                            val task = tasks.find { it.task.id == taskIdFromNotification }
                            if (task != null) {
                                // Ustaw wybrane zadanie i przejdź do ekranu podglądu
                                currentTaskWithAttachments = task
                                navController.navigate("taskView")
                            }
                        }
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
                                // Po kliknięciu na zadanie, przechodzi do ekranu podglądu
                                currentTaskWithAttachments = tasksWithAttachments.find { it.task.id == task.id }
                                navController.navigate("taskView")
                            },
                            onAddTask = {
                                currentTaskWithAttachments = null
                                navController.navigate("taskEdit")
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
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            }
                        )
                    }

                    // Nowy ekran podglądu zadania
                    composable("taskView") {
                        val taskToView = currentTaskWithAttachments
                        if (taskToView != null) {
                            TaskViewScreen(
                                taskWithAttachments = taskToView,
                                onBack = {
                                    navController.navigateUp()
                                },
                                onEdit = {
                                    // Przekierowuje do ekranu edycji
                                    navController.navigate("taskEdit")

                                    // Po powrocie z ekranu edycji, odśwież dane zadania
                                    navController.addOnDestinationChangedListener { _, destination, _ ->
                                        if (destination.route == "taskView" && currentTaskWithAttachments != null) {
                                            // Odśwież zadanie z bazy danych
                                            lifecycleScope.launch {
                                                val taskId = currentTaskWithAttachments?.task?.id
                                                if (taskId != null) {
                                                    withContext(Dispatchers.IO) {
                                                        val refreshedTask = database.attachmentDao().getTaskWithAttachments(taskId)
                                                        withContext(Dispatchers.Main) {
                                                            currentTaskWithAttachments = refreshedTask
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onDelete = {
                                    // Usuwanie zadania
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            database.taskDao().deleteTask(taskToView.task)
                                        }
                                        // Odśwież listę zadań
                                        loadTasks { tasks ->
                                            tasksWithAttachments = tasks
                                        }
                                        // Powrót do listy zadań
                                        navController.navigateUp()
                                    }
                                },
                                onOpenAttachment = { attachment ->
                                    try {
                                        val uri = attachment.filePath.toUri()
                                        if (attachmentHelper.openAttachment(uri, attachment.mimeType)) {
                                            // Udało się otworzyć załącznik
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Brak aplikacji do otwarcia tego typu pliku",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Nie można otworzyć pliku: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        e.printStackTrace()
                                    }
                                }
                            )
                        } else {
                            // Zabezpieczenie przed sytuacją, gdy zadanie nie zostało znalezione
                            navController.navigateUp()
                        }
                    }

                    composable("taskEdit") {
                        TaskEditScreen(
                            taskWithAttachments = currentTaskWithAttachments,
                            onSave = { updatedTask ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val taskId: Int
                                        if (updatedTask.id == 0) {
                                            // Nowe zadanie - najpierw zapisujemy zadanie, aby otrzymać jego ID
                                            taskId = database.taskDao().insertTask(updatedTask.copy(id = 0)).toInt()

                                            // Następnie zapisujemy wszystkie tymczasowe załączniki z poprawnym taskId
                                            currentTaskWithAttachments?.attachments?.forEach { attachment ->
                                                database.attachmentDao().insertAttachment(
                                                    attachment.copy(taskId = taskId)
                                                )
                                            }
                                        } else {
                                            // Aktualizacja istniejącego zadania
                                            taskId = updatedTask.id
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
                                    // Sprawdź, czy zadanie istnieje w bazie danych
                                    val taskId = currentTaskWithAttachments?.task?.id

                                    if (taskId != null && taskId > 0) {
                                        // Dla istniejącego zadania - zapisujemy załącznik bezpośrednio do bazy
                                        withContext(Dispatchers.IO) {
                                            database.attachmentDao().insertAttachment(attachment)

                                            // Odśwież zadanie z załącznikami
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
                                    val uri = attachment.filePath.toUri()
                                    if (!attachmentHelper.openAttachment(uri, attachment.mimeType)) {
                                        Toast.makeText(
                                            context,
                                            "Brak aplikacji do otwarcia tego typu pliku",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Nie można otworzyć pliku: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    e.printStackTrace()
                                }
                            }
                        )
                    }

                    // Nowy ekran ustawień
                    composable("settings") {
                        SettingsScreen(navController = navController)
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
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

