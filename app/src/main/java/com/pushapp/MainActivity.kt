package com.pushapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pushapp.notification.CommentCheckWorker
import com.pushapp.notification.NotificationHelper
import com.pushapp.ui.navigation.AppNavigation
import com.pushapp.ui.theme.PushAppTheme
import com.pushapp.util.ThemePrefs
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.WorkoutViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()

    private var accentKey by mutableStateOf("lime")

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && NotificationHelper.isReminderEnabled(this))
            NotificationHelper.scheduleDailyReminder(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.createCommentNotificationChannel(this)
        requestNotificationPermissionAndSchedule()
        scheduleCommentCheckIfNeeded()
        accentKey = ThemePrefs.getKey(this)

        setContent {
            val option = ThemePrefs.options.find { it.key == accentKey } ?: ThemePrefs.options[0]
            PushAppTheme(accent = option.color, accentDim = option.dim) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        authViewModel    = authViewModel,
                        workoutViewModel = workoutViewModel,
                        onUserLoggedIn   = {
                            if (NotificationHelper.isReminderEnabled(this))
                                NotificationHelper.scheduleDailyReminder(this)
                        },
                        onAccentChanged  = { key ->
                            ThemePrefs.setKey(this, key)
                            accentKey = key
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                if (NotificationHelper.isReminderEnabled(this))
                    NotificationHelper.scheduleDailyReminder(this)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (NotificationHelper.isReminderEnabled(this))
                NotificationHelper.scheduleDailyReminder(this)
        }
    }

    private fun scheduleCommentCheckIfNeeded() {
        if (!NotificationHelper.isCommentNotifEnabled(this)) return
        val request = PeriodicWorkRequestBuilder<CommentCheckWorker>(15, TimeUnit.MINUTES)
            .addTag(NotificationHelper.COMMENT_WORK_TAG)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NotificationHelper.COMMENT_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
