package com.pushapp.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pushapp.MainActivity
import java.util.Calendar
import java.util.TimeZone

object NotificationHelper {
    const val CHANNEL_ID = "workout_reminder"
    const val CHANNEL_ID_COMMENTS = "comment_notif"
    const val NOTIFICATION_ID = 1001
    const val COMMENT_NOTIF_ID = 1002
    const val ALARM_REQUEST_CODE = 2001
    const val COMMENT_WORK_TAG = "comment_check"

    private const val PREFS_NAME = "pushapp_prefs"
    private const val PREF_NOTIF_HOUR = "notif_hour"
    private const val PREF_NOTIF_MINUTE = "notif_minute"
    private const val PREF_REMINDER_ENABLED = "notif_reminder_enabled"
    private const val PREF_COMMENT_NOTIF_ENABLED = "notif_comment_enabled"
    private const val PREF_LAST_COMMENT_CHECK = "notif_last_comment_check"
    const val DEFAULT_HOUR = 20
    const val DEFAULT_MINUTE = 0

    // ── Время напоминания ──────────────────────────────────────────────────

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

    // ── Ежедневное напоминание вкл/выкл ───────────────────────────────────

    fun isReminderEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_REMINDER_ENABLED, true)

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_REMINDER_ENABLED, enabled).apply()
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ── Уведомления о комментариях ─────────────────────────────────────────

    fun isCommentNotifEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_COMMENT_NOTIF_ENABLED, false)

    fun setCommentNotifEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_COMMENT_NOTIF_ENABLED, enabled).apply()
    }

    fun getLastCommentCheck(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(PREF_LAST_COMMENT_CHECK, 0L)

    fun setLastCommentCheck(context: Context, time: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(PREF_LAST_COMMENT_CHECK, time).apply()
    }

    // ── Каналы уведомлений ────────────────────────────────────────────────

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

    fun createCommentNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID_COMMENTS,
            "Комментарии к тренировке",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Уведомления о новых комментариях к твоим тренировкам"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showCommentNotification(context: Context, authorName: String, count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (count == 1)
            "$authorName прокомментировал твою тренировку"
        else
            "$authorName и ещё ${count - 1} чел. прокомментировали твои тренировки"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMMENTS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Новый комментарий")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(COMMENT_NOTIF_ID, notification)
    }

    // ── AlarmManager ──────────────────────────────────────────────────────

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
