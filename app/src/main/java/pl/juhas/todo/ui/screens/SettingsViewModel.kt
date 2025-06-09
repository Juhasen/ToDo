package pl.juhas.todo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.juhas.todo.database.Category
import pl.juhas.todo.utils.SettingsManager

class SettingsViewModel : ViewModel() {

    private val _notificationAdvanceTime = MutableStateFlow(30L * 60L * 1000L) // domyślnie 30 minut w ms
    val notificationAdvanceTime: StateFlow<Long> = _notificationAdvanceTime

    // Stan dla ukrywania zakończonych zadań
    private val _hideDoneTasks = MutableStateFlow(false)
    val hideDoneTasks: StateFlow<Boolean> = _hideDoneTasks

    // Stan dla wybranych kategorii
    private val _selectedCategories = MutableStateFlow(
        mutableMapOf<Category, Boolean>().apply {
            Category.entries.forEach { category ->
                this[category] = true // domyślnie wszystkie kategorie są wybrane
            }
        }
    )
    val selectedCategories: StateFlow<Map<Category, Boolean>> = _selectedCategories

    fun loadSettings(settingsManager: SettingsManager) {
        viewModelScope.launch {
            // Załaduj czas wyprzedzenia powiadomień
            val advanceTime = settingsManager.notificationAdvanceTime.first()
            _notificationAdvanceTime.value = advanceTime

            // Załaduj opcję ukrywania zakończonych zadań
            val hideDone = settingsManager.hideDoneTasks.first()
            _hideDoneTasks.value = hideDone

            // Załaduj opcje wybranych kategorii
            val categories = mutableMapOf<Category, Boolean>()
            Category.entries.forEach { category ->
                categories[category] = settingsManager.getCategorySelected(category).first()
            }
            _selectedCategories.value = categories
        }
    }

    fun saveNotificationAdvanceTime(minutes: Int, settingsManager: SettingsManager) {
        val timeInMillis = minutes.toLong() * 60L * 1000L
        _notificationAdvanceTime.value = timeInMillis
        viewModelScope.launch {
            settingsManager.setNotificationAdvanceTime(timeInMillis)
        }
    }

    fun setHideDoneTasks(hide: Boolean, settingsManager: SettingsManager) {
        _hideDoneTasks.value = hide
        viewModelScope.launch {
            settingsManager.setHideDoneTasks(hide)
        }
    }

    fun setCategorySelected(category: Category, selected: Boolean, settingsManager: SettingsManager) {
        val updatedMap = _selectedCategories.value.toMutableMap()
        updatedMap[category] = selected
        _selectedCategories.value = updatedMap

        viewModelScope.launch {
            settingsManager.setCategorySelected(category, selected)
        }
    }

    fun setAllCategoriesSelected(selected: Boolean, settingsManager: SettingsManager) {
        val updatedMap = mutableMapOf<Category, Boolean>()
        Category.entries.forEach { category ->
            updatedMap[category] = selected
        }
        _selectedCategories.value = updatedMap

        viewModelScope.launch {
            settingsManager.setAllCategoriesSelected(selected)
        }
    }

    fun getMinutesText(): String {
        val minutes = _notificationAdvanceTime.value / (60 * 1000)
        return minutes.toString()
    }
}
