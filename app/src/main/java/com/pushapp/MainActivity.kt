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
import com.pushapp.notification.NotificationHelper
import com.pushapp.ui.navigation.AppNavigation
import com.pushapp.ui.theme.PushAppTheme
import com.pushapp.util.ThemePrefs
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()

    private var accentKey by mutableStateOf("lime")

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) NotificationHelper.scheduleDailyReminder(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionAndSchedule()
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
                        onUserLoggedIn   = { NotificationHelper.scheduleDailyReminder(this) },
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
                NotificationHelper.scheduleDailyReminder(this)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationHelper.scheduleDailyReminder(this)
        }
    }
}
