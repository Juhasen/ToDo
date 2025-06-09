package pl.juhas.todo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.juhas.todo.utils.SettingsManager

class SettingsViewModel : ViewModel() {

    private val _notificationAdvanceTime = MutableStateFlow(30L * 60L * 1000L) // domyślnie 30 minut w ms
    val notificationAdvanceTime: StateFlow<Long> = _notificationAdvanceTime

    fun loadSettings(settingsManager: SettingsManager) {
        viewModelScope.launch {
            val advanceTime = settingsManager.notificationAdvanceTime.first()
            _notificationAdvanceTime.value = advanceTime
        }
    }

    fun saveNotificationAdvanceTime(minutes: Int, settingsManager: SettingsManager) {
        val timeInMillis = minutes.toLong() * 60L * 1000L
        _notificationAdvanceTime.value = timeInMillis
        viewModelScope.launch {
            settingsManager.setNotificationAdvanceTime(timeInMillis)
        }
    }

    fun getMinutesText(): String {
        val minutes = _notificationAdvanceTime.value / (60 * 1000)
        return minutes.toString()
    }
}
