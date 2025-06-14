package pl.juhas.todo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import android.widget.Toast
import pl.juhas.todo.database.*
import pl.juhas.todo.ui.composables.AttachmentItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskWithAttachments: TaskWithAttachments?,
    onSave: (Task, List<Attachment>) -> Unit,   // Zmodyfikowane, aby obsłużyć również listę załączników
    onCancel: () -> Unit,
    onAddAttachment: (Attachment) -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onOpenAttachment: (Attachment) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(taskWithAttachments?.task?.title ?: "") }
    var description by remember { mutableStateOf(taskWithAttachments?.task?.description ?: "") }
    var status by remember { mutableStateOf(taskWithAttachments?.task?.status ?: TaskStatus.TODO) }
    var notify by remember { mutableStateOf(taskWithAttachments?.task?.notify == true) }
    var notifyAtDate by remember { mutableStateOf(taskWithAttachments?.task?.finishAt?.let { Date(it) } ?: Date()) }
    var category by remember { mutableStateOf(taskWithAttachments?.task?.category ?: Category.NORMAL) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    // Przechowywanie istniejących oraz tymczasowych załączników
    val existingAttachments = taskWithAttachments?.attachments ?: emptyList()
    val tempAttachments = remember { mutableStateListOf<Attachment>() }

    // Łączymy wszystkie załączniki do wyświetlenia
    val allAttachments = if (taskWithAttachments?.task?.id != null) {
        existingAttachments
    } else {
        tempAttachments
    }

    // Launcher dla wybierania mediów (zdjęć)
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            if (documentFile != null) {
                val mimeType = documentFile.type ?: "application/octet-stream"
                val fileName = documentFile.name ?: "file_${System.currentTimeMillis()}"
                val fileSize = documentFile.length()

                val newAttachment = Attachment(
                    taskId = taskWithAttachments?.task?.id ?: 0,
                    fileName = fileName,
                    filePath = uri.toString(),
                    mimeType = mimeType,
                    fileSize = fileSize,
                    createdAt = System.currentTimeMillis()
                )

                if (taskWithAttachments?.task?.id != null) {
                    onAddAttachment(newAttachment)
                } else {
                    tempAttachments.add(newAttachment)
                }
            }
        }
    }

    // Launcher dla wybierania dokumentów
    val pickDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            if (documentFile != null) {
                val mimeType = documentFile.type ?: "application/octet-stream"
                val fileName = documentFile.name ?: "file_${System.currentTimeMillis()}"
                val fileSize = documentFile.length()

                // Uzyskaj stały dostęp do pliku
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                val newAttachment = Attachment(
                    taskId = taskWithAttachments?.task?.id ?: 0,
                    fileName = fileName,
                    filePath = uri.toString(),
                    mimeType = mimeType,
                    fileSize = fileSize,
                    createdAt = System.currentTimeMillis()
                )

                if (taskWithAttachments?.task?.id != null) {
                    onAddAttachment(newAttachment)
                } else {
                    tempAttachments.add(newAttachment)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (taskWithAttachments?.task?.id != null) "Edycja zadania" else "Nowe zadanie")
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Powrót")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Sprawdź czy tytuł nie jest pusty
                        if (title.isBlank()) {
                            Toast.makeText(context, "Tytuł zadania nie może być pusty!", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }

                        val updatedTask = if (taskWithAttachments?.task != null) {
                            taskWithAttachments.task.copy(
                                title = title,
                                description = description,
                                status = status,
                                notify = notify,
                                finishAt = notifyAtDate.time,
                                category = category
                            )
                        } else {
                            Task(
                                id = 0, // Room wygeneruje nowe ID automatycznie
                                title = title,
                                description = description,
                                createdAt = System.currentTimeMillis(),
                                status = status,
                                notify = notify,
                                finishAt = notifyAtDate.time,
                                category = category
                            )
                        }
                        onSave(updatedTask, tempAttachments)
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
                .verticalScroll(rememberScrollState())
        ) {
            // Tytuł zadania
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tytuł") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Opis zadania
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status zadania
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Status:")
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = status == TaskStatus.DONE,
                    onCheckedChange = { isChecked ->
                        status = if (isChecked) TaskStatus.DONE else TaskStatus.TODO
                    }
                )
                Text(
                    text = if (status == TaskStatus.DONE) "Zakończone" else "Do zrobienia",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wyświetlanie daty utworzenia zadania
            val creationDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            Text(
                text = "Data utworzenia: ${creationDateFormat.format(Date(taskWithAttachments?.task?.createdAt ?: System.currentTimeMillis()))}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Powiadomienie
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Powiadomienie:")
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = notify,
                    onCheckedChange = { isChecked ->
                        notify = isChecked
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Data i czas zakończenia - zawsze widoczne, niezależnie od powiadomień
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Data wykonania: ${dateFormat.format(notifyAtDate)}",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Wybierz datę")
                }
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "Wybierz czas")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kategoria
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kategoria:")
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { showCategoryDialog = true }
                ) {
                    Text(category.name)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sekcja załączników
            Text(
                text = "Załączniki",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista załączników
            if (allAttachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allAttachments) { attachment ->
                        AttachmentItem(
                            attachment = attachment,
                            onRemove = {
                                if (taskWithAttachments?.task?.id != null) {
                                    onRemoveAttachment(attachment)
                                } else {
                                    tempAttachments.remove(attachment)
                                }
                            },
                            onOpen = { onOpenAttachment(attachment) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Przyciski dodawania załączników
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Dodaj zdjęcie")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dodaj zdjęcie")
                }

                Button(
                    onClick = {
                        pickDocument.launch(arrayOf("*/*"))
                    }
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Dodaj plik")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dodaj plik")
                }
            }
        }

        // Dialog wyboru kategorii
        if (showCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                title = { Text("Wybierz kategorię") },
                text = {
                    Column {
                        Category.entries.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = cat == category,
                                    onClick = {
                                        category = cat
                                        showCategoryDialog = false
                                    }
                                )
                                Text(
                                    text = cat.name,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryDialog = false }) {
                        Text("Anuluj")
                    }
                }
            )
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = it

                            val currentCalendar = Calendar.getInstance()
                            currentCalendar.time = notifyAtDate

                            // Zachowaj aktualną godzinę i minutę
                            calendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))

                            notifyAtDate = calendar.time
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Anuluj")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Time Picker Dialog
        if (showTimePicker) {
            val timePickerState = rememberTimePickerState()
            Dialog(onDismissRequest = { showTimePicker = false }) {
                Surface(
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimePicker(state = timePickerState)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showTimePicker = false }) {
                                Text("Anuluj")
                            }

                            Button(onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.time = notifyAtDate

                                calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                calendar.set(Calendar.MINUTE, timePickerState.minute)

                                notifyAtDate = calendar.time
                                showTimePicker = false
                            }) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}
