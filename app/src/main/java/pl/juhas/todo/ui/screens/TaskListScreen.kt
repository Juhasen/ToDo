package pl.juhas.todo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SearchBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.juhas.todo.database.Category
import pl.juhas.todo.database.Task
import pl.juhas.todo.database.TaskStatus
import pl.juhas.todo.database.TaskWithAttachments
import pl.juhas.todo.ui.composables.TaskItem
import pl.juhas.todo.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    var isSearchActive by remember { mutableStateOf(false) }

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
            taskWithAttachments.task.title.contains(searchQuery, ignoreCase = true) ||
            (taskWithAttachments.task.description?.contains(searchQuery, ignoreCase = true) == true)
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
            if (isSearchActive) {
                // Pasek wyszukiwania gdy aktywny
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { /* Nie trzeba nic robić, wyszukiwanie działa od razu */ },
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    placeholder = { Text("Szukaj zadań...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Szukaj") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Wyniki wyszukiwania mogą być tutaj wyświetlane jako podpowiedzi
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        val suggestedTasks = filteredTasks.take(5)
                        items(suggestedTasks) { taskWithAttachments ->
                            Text(
                                text = taskWithAttachments.task.title,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clickable {
                                        isSearchActive = false
                                        onTaskClick(taskWithAttachments.task)
                                    }
                            )
                        }
                    }
                }
            } else {
                // Normalny TopAppBar gdy wyszukiwanie nie jest aktywne
                TopAppBar(
                    title = { Text("TODO LIST") },
                    actions = {
                        // Przycisk wyszukiwania
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Szukaj")
                        }

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
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj zadanie")
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = filteredTasks.isEmpty(),
            label = "EmptyStateAnimation"
        ) { isEmpty ->
            if (isEmpty) {
                // Widok gdy lista jest pusta
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when {
                            tasks.isEmpty() -> "Brak zadań. Dodaj nowe zadanie."
                            hideDoneTasks && tasks.all { it.task.status == TaskStatus.DONE } ->
                                "Wszystkie zadania są zakończone. Wyłącz filtr, aby je zobaczyć."
                            else -> "Brak zadań pasujących do ustawionych filtrów."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (tasks.isEmpty()) {
                        Button(
                            onClick = onAddTask,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dodaj nowe zadanie")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Element wyszukiwania na górze listy jeśli wyszukiwanie nie jest aktywne
                    if (!isSearchActive) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSearchActive = true },
                                placeholder = { Text("Szukaj zadań...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Szukaj") },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                                        }
                                    }
                                },
                                singleLine = true,
                                enabled = false // Wyłączone, aktywacja poprzez kliknięcie
                            )
                        }
                    }

                    // Filtrowanie kategorii - rozwijany panel
                    item {
                        AnimatedVisibility(
                            visible = showCategoryFilter,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Filtruj kategorie:",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Category.entries.forEach { category ->
                                            FilterChip(
                                                selected = selectedCategories[category] != false,
                                                onClick = {
                                                    selectedCategories[category] = !(selectedCategories[category] ?: true)
                                                    saveFilterPreferences()
                                                },
                                                label = { Text(category.name) },
                                                leadingIcon = if (selectedCategories[category] != false) {
                                                    {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null
                                            )
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
                    }

                    // Wyświetlanie przefiltrowanych zadań z animacją
                    items(
                        items = filteredTasks,
                        key = { it.task.id }
                    ) { taskWithAttachments ->
                        AnimatedVisibility(
                            visible = true,
                            enter = expandVertically() + fadeIn(),
                            modifier = Modifier.animateItem()
                        ) {
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
    }
}
