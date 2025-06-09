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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.juhas.todo.database.Category
import pl.juhas.todo.database.Task
import pl.juhas.todo.database.TaskWithAttachments
import pl.juhas.todo.ui.composables.TaskItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskListScreen(
    tasks: List<TaskWithAttachments>,
    onTaskClick: (Task) -> Unit,
    onAddTask: () -> Unit,
    onTaskStatusChange: (Task) -> Unit,
    onSettingsClick: () -> Unit
) {
    // Stan dla wyszukiwarki
    var searchQuery by remember { mutableStateOf("") }

    // Stan dla filtra kategorii
    var showCategoryFilter by remember { mutableStateOf(false) }

    // Stan dla wybranych kategorii (true = wybrana, false = niewybrana)
    val selectedCategories = remember {
        mutableStateMapOf<Category, Boolean>().apply {
            Category.entries.forEach { category ->
                this[category] = true
            }
        }
    }

    val filteredTasks = tasks.filter { taskWithAttachments ->
        val matchesSearch = if (searchQuery.isEmpty()) {
            true
        } else {
            taskWithAttachments.task.title.contains(searchQuery, ignoreCase = true)
        }

        val matchesCategory = selectedCategories[taskWithAttachments.task.category] != false

        // Zadanie musi spełniać oba warunki
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TODO LIST") },
                actions = {
                    IconButton(onClick = { showCategoryFilter = !showCategoryFilter }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtruj kategorie")
                    }
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
        }
    }
}

