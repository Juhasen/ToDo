package pl.juhas.todo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import pl.juhas.todo.database.Category
import pl.juhas.todo.database.Task
import pl.juhas.todo.database.TaskStatus
import pl.juhas.todo.database.TaskWithAttachments
import pl.juhas.todo.ui.screens.EmptyDetailScreen
import pl.juhas.todo.ui.screens.SettingsScreen
import pl.juhas.todo.ui.screens.TaskEditScreen
import pl.juhas.todo.ui.screens.TaskListScreen
import pl.juhas.todo.ui.screens.TaskViewScreen
import pl.juhas.todo.ui.theme.TODOTheme
import pl.juhas.todo.ui.utils.DeviceUtils
import pl.juhas.todo.utils.AttachmentHelper
import pl.juhas.todo.utils.NotificationHelper
import pl.juhas.todo.utils.SettingsManager

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var attachmentHelper: AttachmentHelper
    private lateinit var settingsManager: SettingsManager
    private lateinit var notificationHelper: NotificationHelper

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

        // Inicjalizacja pomocnika powiadomień
        notificationHelper = NotificationHelper(this)

        // Sprawdzenie i żądanie uprawnień do powiadomień
        checkNotificationPermission()

        setContent {
            TODOTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // Wykrywanie czy urządzenie jest tabletem
                val isTablet = DeviceUtils.isTablet()

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
                                if (!isTablet) {
                                    navController.navigate("taskView")
                                }
                            }
                        }
                    }
                }

                // Na tabletach wyświetlamy inny layout (dwukolumnowy)
                if (isTablet) {
                    // Stan do śledzenia, czy aktualnie jesteśmy w trybie edycji lub ustawień
                    var rightColumnMode by remember { mutableStateOf<String?>(null) } // null, "edit", "settings"

                    // Układ dwukolumnowy dla tabletu
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Lewa kolumna - lista zadań (1/3 szerokości ekranu)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            TaskListScreen(
                                tasks = tasksWithAttachments,
                                onTaskClick = { task ->
                                    currentTaskWithAttachments = tasksWithAttachments.find { it.task.id == task.id }
                                    // Po wybraniu zadania, wyjdź z trybu edycji/ustawień
                                    rightColumnMode = null
                                },
                                onAddTask = {
                                    currentTaskWithAttachments = null
                                    rightColumnMode = "edit"
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
                                    rightColumnMode = "settings"
                                }
                            )
                        }

                        // Separator
                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Prawa kolumna - szczegóły zadania, edycja lub ustawienia (2/3 szerokości ekranu)
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .padding(start = 8.dp)
                        ) {
                            when (rightColumnMode) {
                                "edit" -> {
                                    // Tryb edycji w prawej kolumnie
                                    TaskEditScreen(
                                        taskWithAttachments = currentTaskWithAttachments,
                                        onSave = { updatedTask, tempAttachments ->
                                            lifecycleScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    val taskId: Int
                                                    if (updatedTask.id == 0) {
                                                        // Nowe zadanie - najpierw zapisujemy zadanie, aby otrzymać jego ID
                                                        taskId = database.taskDao().insertTask(updatedTask.copy(id = 0)).toInt()

                                                        // Teraz zapisujemy wszystkie załączniki związane z nowym zadaniem
                                                        tempAttachments.forEach { attachment ->
                                                            // Aktualizujemy ID zadania dla każdego załącznika
                                                            val updatedAttachment = attachment.copy(taskId = taskId)
                                                            database.attachmentDao().insertAttachment(updatedAttachment)
                                                        }
                                                    } else {
                                                        // Aktualizacja istniejącego zadania
                                                        taskId = updatedTask.id
                                                        database.taskDao().updateTask(updatedTask)
                                                    }

                                                    // Planowanie powiadomienia tylko jeśli powiadomienie jest włączone
                                                    if (updatedTask.notify) {
                                                        notificationHelper.scheduleNotification(
                                                            updatedTask.copy(id = taskId),
                                                            settingsManager.notificationAdvanceTime.first()
                                                        )
                                                    } else {
                                                        // Anuluj powiadomienie, jeśli zostało wyłączone
                                                        notificationHelper.cancelNotification(taskId)
                                                    }
                                                }

                                                // Odśwież listę zadań
                                                loadTasks { tasks ->
                                                    tasksWithAttachments = tasks

                                                    // Jeśli dodano nowe zadanie, wybierz je
                                                    if (updatedTask.id == 0) {
                                                        // Znajdź nowododane zadanie (ostatnie na liście)
                                                        val newTask = tasks.maxByOrNull { it.task.id }
                                                        if (newTask != null) {
                                                            currentTaskWithAttachments = newTask
                                                        }
                                                    } else {
                                                        // Zaktualizuj aktualne zadanie
                                                        val refreshedTask = tasks.find { it.task.id == updatedTask.id }
                                                        if (refreshedTask != null) {
                                                            currentTaskWithAttachments = refreshedTask
                                                        }
                                                    }
                                                }

                                                // Po zapisaniu wyjdź z trybu edycji
                                                rightColumnMode = null
                                            }
                                        },
                                        onCancel = {
                                            // Wyjście z trybu edycji
                                            rightColumnMode = null
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
                                                    val updatedAttachments =
                                                        currentTaskWithAttachments?.attachments?.toMutableList() ?: mutableListOf()
                                                    updatedAttachments.add(attachment)

                                                    withContext(Dispatchers.Main) {
                                                        currentTaskWithAttachments = TaskWithAttachments(
                                                            task = currentTaskWithAttachments?.task ?: Task(
                                                                id = 0,
                                                                title = "",
                                                                description = null,
                                                                createdAt = System.currentTimeMillis(),
                                                                status = TaskStatus.TODO,
                                                                notify = false,
                                                                finishAt = System.currentTimeMillis(),
                                                                category = Category.NORMAL
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
                                                        val updatedAttachments =
                                                            currentTaskWithAttachments?.attachments?.toMutableList() ?: mutableListOf()
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

                                "settings" -> {
                                    // Tryb ustawień w prawej kolumnie
                                    SettingsScreen(
                                        navController = navController,
                                        onBackPressed = {
                                            rightColumnMode = null
                                        }
                                    )
                                }

                                else -> {
                                    // Tryb podglądu zadania lub pusty ekran
                                    if (currentTaskWithAttachments != null) {
                                        TaskViewScreen(
                                            taskWithAttachments = currentTaskWithAttachments!!,
                                            onBack = { /* W widoku tabletu przycisk powrotu nie jest potrzebny */ },
                                            onEdit = {
                                                // Przejdź do trybu edycji
                                                rightColumnMode = "edit"
                                            },
                                            onDelete = {
                                                lifecycleScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        database.taskDao().deleteTask(currentTaskWithAttachments!!.task)
                                                    }
                                                    // Odśwież listę zadań
                                                    loadTasks { tasks ->
                                                        tasksWithAttachments = tasks
                                                    }
                                                }

                                                // Po usunięciu wyczyść wybrane zadanie
                                                currentTaskWithAttachments = tasksWithAttachments.firstOrNull()
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
                                    } else {
                                        // Komunikat, gdy nie wybrano żadnego zadania
                                        EmptyDetailScreen()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Standardowa nawigacja dla telefonów
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
                                onSave = { updatedTask, tempAttachments ->
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            val taskId: Int
                                            if (updatedTask.id == 0) {
                                                // Nowe zadanie - najpierw zapisujemy zadanie, aby otrzymać jego ID
                                                taskId = database.taskDao().insertTask(updatedTask.copy(id = 0)).toInt()

                                                // Teraz zapisujemy wszystkie załączniki związane z nowym zadaniem
                                                tempAttachments.forEach { attachment ->
                                                    // Aktualizujemy ID zadania dla każdego załącznika
                                                    val updatedAttachment = attachment.copy(taskId = taskId)
                                                    database.attachmentDao().insertAttachment(updatedAttachment)
                                                }
                                            } else {
                                                // Aktualizacja istniejącego zadania
                                                taskId = updatedTask.id
                                                database.taskDao().updateTask(updatedTask)
                                            }

                                            // Planowanie powiadomienia tylko jeśli powiadomienie jest włączone
                                            if (updatedTask.notify) {
                                                notificationHelper.scheduleNotification(updatedTask.copy(id = taskId),
                                                    settingsManager.notificationAdvanceTime.first()
                                                )
                                            } else {
                                                // Anuluj powiadomienie, jeśli zostało wyłączone
                                                notificationHelper.cancelNotification(taskId)
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
                                                        createdAt = System.currentTimeMillis(),
                                                        status = TaskStatus.TODO,
                                                        notify = false,
                                                        finishAt = System.currentTimeMillis(),
                                                        category = Category.NORMAL
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
