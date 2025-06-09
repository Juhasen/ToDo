package pl.juhas.todo.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.juhas.todo.database.Category

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val NOTIFICATION_ADVANCE_TIME = longPreferencesKey("notification_advance_time")
        private const val DEFAULT_ADVANCE_TIME = 30L * 60L * 1000L // 30 minut w milisekundach

        // Klucz dla opcji ukrywania zakończonych zadań
        private val HIDE_DONE_TASKS = booleanPreferencesKey("hide_done_tasks")

        // Klucz prefiksowy dla wybranych kategorii
        private const val SELECTED_CATEGORY_PREFIX = "selected_category_"
    }

    // Pobieranie czasu wyprzedzenia powiadomień (w milisekundach)
    val notificationAdvanceTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_ADVANCE_TIME] ?: DEFAULT_ADVANCE_TIME
        }

    // Zapisywanie czasu wyprzedzenia powiadomień (w milisekundach)
    suspend fun setNotificationAdvanceTime(timeInMillis: Long) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ADVANCE_TIME] = timeInMillis
        }
    }

    // Pobieranie opcji ukrywania zakończonych zadań
    val hideDoneTasks: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HIDE_DONE_TASKS] ?: false // domyślnie pokazuj wszystkie zadania
        }

    // Zapisywanie opcji ukrywania zakończonych zadań
    suspend fun setHideDoneTasks(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HIDE_DONE_TASKS] = hide
        }
    }

    // Pobieranie statusu filtrowania dla konkretnej kategorii
    fun getCategorySelected(category: Category): Flow<Boolean> {
        val key = booleanPreferencesKey(SELECTED_CATEGORY_PREFIX + category.name)
        return context.dataStore.data
            .map { preferences ->
                preferences[key] ?: true // domyślnie wszystkie kategorie są wybrane
            }
    }

    // Zapisywanie statusu filtrowania dla konkretnej kategorii
    suspend fun setCategorySelected(category: Category, selected: Boolean) {
        val key = booleanPreferencesKey(SELECTED_CATEGORY_PREFIX + category.name)
        context.dataStore.edit { preferences ->
            preferences[key] = selected
        }
    }

    // Zapisywanie statusu filtrowania dla wszystkich kategorii
    suspend fun setAllCategoriesSelected(selected: Boolean) {
        context.dataStore.edit { preferences ->
            for (category in Category.entries) {
                val key = booleanPreferencesKey(SELECTED_CATEGORY_PREFIX + category.name)
                preferences[key] = selected
            }
        }
    }
}
