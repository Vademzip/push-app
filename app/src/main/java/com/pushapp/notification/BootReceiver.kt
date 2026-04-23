package com.pushapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth

// Перепланирует будильник после перезагрузки телефона
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            FirebaseAuth.getInstance().currentUser != null
        ) {
            if (NotificationHelper.isReminderEnabled(context))
                NotificationHelper.scheduleDailyReminder(context)
        }
    }
}
