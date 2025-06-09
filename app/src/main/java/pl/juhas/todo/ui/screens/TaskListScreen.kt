package pl.juhas.todo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.juhas.todo.database.Category
import pl.juhas.todo.database.Task
import pl.juhas.todo.database.TaskStatus
import pl.juhas.todo.database.TaskWithAttachments
import pl.juhas.todo.ui.composables.TaskItem
import pl.juhas.todo.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskListScreen(
    tasks: List<TaskWithAttachments>,
    onTaskClick: (Task) -> Unit,
    onAddTask: () -> Unit,
    onTaskStatusChange: (Task) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    // Stan dla wyszukiwarki
    var searchQuery by remember { mutableStateOf("") }

    // Stan dla filtra kategorii
    var showCategoryFilter by remember { mutableStateOf(false) }

    // Pobieranie domyślnych ustawień filtrowania z SettingsManager
    var hideDoneTasks by remember { mutableStateOf(false) }
    val selectedCategories = remember { mutableStateMapOf<Category, Boolean>() }

    // Załaduj domyślne ustawienia filtrowania
    LaunchedEffect(Unit) {
        // Pobierz domyślną wartość dla ukrywania zakończonych zadań
        hideDoneTasks = settingsManager.hideDoneTasks.first()

        // Pobierz domyślne wartości dla filtrowania kategorii
        Category.entries.forEach { category ->
            selectedCategories[category] = settingsManager.getCategorySelected(category).first()
        }
    }

    // Zapisz ustawienia filtrowania do SettingsManager przy zmianie
    fun saveFilterPreferences() {
        coroutineScope.launch {
            settingsManager.setHideDoneTasks(hideDoneTasks)
            Category.entries.forEach { category ->
                settingsManager.setCategorySelected(category, selectedCategories[category] != false)
            }
        }
    }

    // Filtrowanie zadań na podstawie wyszukiwanego tekstu, wybranych kategorii i ukrywania zakończonych zadań
    val filteredTasks = tasks.filter { taskWithAttachments ->
        // Sprawdź, czy tytuł zawiera wyszukiwany tekst
        val matchesSearch = if (searchQuery.isEmpty()) {
            true
        } else {
            taskWithAttachments.task.title.contains(searchQuery, ignoreCase = true)
        }

        // Sprawdź, czy kategoria zadania jest wśród wybranych kategorii
        val matchesCategory = selectedCategories[taskWithAttachments.task.category] != false

        // Sprawdź, czy zadanie nie jest zakończone lub czy nie ukrywamy zakończonych zadań
        val matchesStatus = !(hideDoneTasks && taskWithAttachments.task.status == TaskStatus.DONE)

        // Zadanie musi spełniać wszystkie warunki
        matchesSearch && matchesCategory && matchesStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TODO LIST") },
                actions = {
                    // Przycisk do włączania/wyłączania ukrywania zakończonych zadań
                    IconButton(onClick = {
                        hideDoneTasks = !hideDoneTasks
                        saveFilterPreferences()
                    }) {
                        Icon(
                            imageVector = if (hideDoneTasks) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (hideDoneTasks) "Pokaż zakończone zadania" else "Ukryj zakończone zadania",
                            tint = if (hideDoneTasks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Przycisk do filtrowania kategorii
                    IconButton(onClick = { showCategoryFilter = !showCategoryFilter }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtruj kategorie")
                    }

                    // Przycisk ustawień
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj zadanie")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Element wyszukiwania na górze listy
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Szukaj zadań...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Szukaj") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Filtrowanie kategorii - rozwijany panel
            item {
                AnimatedVisibility(
                    visible = showCategoryFilter,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Filtruj kategorie:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Category.entries.forEach { category ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedCategories[category] != false,
                                        onCheckedChange = { isChecked ->
                                            selectedCategories[category] = isChecked
                                            saveFilterPreferences()
                                        }
                                    )
                                    Text(category.name)
                                }
                            }
                        }

                        // Przyciski "Zaznacz wszystkie" i "Odznacz wszystkie"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    Category.entries.forEach { category ->
                                        selectedCategories[category] = true
                                    }
                                    saveFilterPreferences()
                                }
                            ) {
                                Text("Zaznacz wszystkie")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    Category.entries.forEach { category ->
                                        selectedCategories[category] = false
                                    }
                                    saveFilterPreferences()
                                }
                            ) {
                                Text("Odznacz wszystkie")
                            }
                        }
                    }
                }
            }

            // Wyświetlanie przefiltrowanych zadań
            items(filteredTasks) { taskWithAttachments ->
                TaskItem(
                    taskWithAttachments = taskWithAttachments,
                    onClick = { onTaskClick(taskWithAttachments.task) },
                    onStatusChange = { onTaskStatusChange(taskWithAttachments.task) }
                )
            }

            // Informacja o braku zadań po filtrowaniu
            if (filteredTasks.isEmpty()) {
                item {
                    Text(
                        text = when {
                            tasks.isEmpty() -> "Brak zadań. Dodaj nowe zadanie."
                            hideDoneTasks && tasks.all { it.task.status == TaskStatus.DONE } ->
                                "Wszystkie zadania są zakończone. Wyłącz filtr, aby je zobaczyć."
                            else -> "Brak zadań pasujących do ustawionych filtrów."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }
        }
    }
}
