package pl.juhas.todo.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import pl.juhas.todo.database.Task
import java.util.Date

class NotificationHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "task_notifications"
        const val TASK_ID_KEY = "task_id"
        const val TASK_TITLE_KEY = "task_title"
        const val TASK_DESCRIPTION_KEY = "task_description"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "Task Notifications"
        val description = "Notifications for tasks"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            this.description = description
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleNotification(task: Task, advanceTimeMillis: Long) {
        // Sprawdzamy czy task ma ustawiony czas zakończenia
        val finishAt = task.finishAt

        // Sprawdzamy czy powiadomienie jest włączone
        if (!task.notify) {
            Log.d("NotificationHelper", "Notifications are disabled for task: ${task.title}")
            return
        }

        // Sprawdzamy uprawnienie do ustawiania alarmów
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e("NotificationHelper", "Missing SCHEDULE_EXACT_ALARM permission")
            Toast.makeText(context, "Proszę włączyć uprawnienie 'Dokładne alarmy' w ustawieniach.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            return
        }

        // Obliczamy czas powiadomienia (czas zakończenia - wyprzedzenie)
        val notificationTime = finishAt - advanceTimeMillis
        val currentTime = System.currentTimeMillis()

        // Sprawdzamy czy czas powiadomienia nie minął
        if (notificationTime <= currentTime) {
            Log.d("NotificationHelper", "Notification time has already passed for task: ${task.title}")
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(TASK_ID_KEY, task.id)
            putExtra(TASK_TITLE_KEY, task.title)
            putExtra(TASK_DESCRIPTION_KEY, task.description ?: "")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            notificationTime,
            pendingIntent
        )

        Log.d("NotificationHelper", "Scheduled notification for task: ${task.title} at ${Date(notificationTime)}")
    }

    fun cancelNotification(taskId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
