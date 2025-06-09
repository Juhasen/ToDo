package pl.juhas.todo.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val NOTIFICATION_ADVANCE_TIME = longPreferencesKey("notification_advance_time")
        private const val DEFAULT_ADVANCE_TIME = 30L * 60L * 1000L // 30 minut w milisekundach
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
}
