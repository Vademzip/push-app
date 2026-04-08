package com.pushapp.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pushapp.MainActivity
import java.util.Calendar
import java.util.TimeZone

object NotificationHelper {
    const val CHANNEL_ID = "workout_reminder"
    const val NOTIFICATION_ID = 1001
    const val ALARM_REQUEST_CODE = 2001

    private const val PREFS_NAME = "pushapp_prefs"
    private const val PREF_NOTIF_HOUR = "notif_hour"
    private const val PREF_NOTIF_MINUTE = "notif_minute"
    const val DEFAULT_HOUR = 20
    const val DEFAULT_MINUTE = 0

    fun getNotificationTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_NOTIF_HOUR, DEFAULT_HOUR) to prefs.getInt(PREF_NOTIF_MINUTE, DEFAULT_MINUTE)
    }

    fun saveNotificationTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(PREF_NOTIF_HOUR, hour)
            .putInt(PREF_NOTIF_MINUTE, minute)
            .apply()
    }

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминание о тренировке",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Ежедневное напоминание отчитаться о тренировке"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (hour, minute) = getNotificationTime(context)
        val triggerTime = getNextTriggerMillis(hour, minute)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun getNextTriggerMillis(hour: Int, minute: Int): Long {
        val msk = TimeZone.getTimeZone("Europe/Moscow")
        val cal = Calendar.getInstance(msk)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
