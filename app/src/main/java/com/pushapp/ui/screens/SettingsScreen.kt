package com.pushapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.BuildConfig
import com.pushapp.notification.NotificationHelper
import com.pushapp.util.AppUpdater
import com.pushapp.util.UpdateInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val (savedHour, savedMinute) = remember { NotificationHelper.getNotificationTime(context) }
    val timePickerState = rememberTimePickerState(
        initialHour   = savedHour,
        initialMinute = savedMinute,
        is24Hour      = true
    )
    var timeSaved by remember { mutableStateOf(false) }

    // Состояние проверки обновлений
    var updateChecking  by remember { mutableStateOf(false) }
    var updateInfo      by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateChecked   by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Время напоминания ──────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Text(
                text     = "Время напоминания",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text     = "Уведомление придёт каждый день в выбранное время (московское время)",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            TimePicker(state = timePickerState)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    NotificationHelper.saveNotificationTime(context, timePickerState.hour, timePickerState.minute)
                    NotificationHelper.scheduleDailyReminder(context)
                    timeSaved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
            AnimatedVisibility(timeSaved) {
                Text(
                    text      = "Напоминание в %02d:%02d".format(timePickerState.hour, timePickerState.minute),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.primary,
                    modifier  = Modifier.padding(top = 8.dp)
                )
            }

            // ── Обновление ────────────────────────────────────────────────
            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text     = "Обновление",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Текущая версия", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Результат проверки
                    when {
                        updateChecked && updateInfo == null -> Text(
                            text  = "Актуальная версия, обновлений нет ✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        updateInfo != null -> {
                            Text(
                                text  = "Доступна версия ${updateInfo!!.versionName}!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (updateInfo!!.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text  = updateInfo!!.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { AppUpdater.downloadAndInstall(context, updateInfo!!) },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(50.dp)
                            ) {
                                Text("Скачать и установить")
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                updateChecking = true
                                updateChecked  = false
                                updateInfo     = null
                                updateInfo     = AppUpdater.checkForUpdate()
                                updateChecked  = true
                                updateChecking = false
                            }
                        },
                        enabled  = !updateChecking,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(50.dp)
                    ) {
                        if (updateChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (updateChecking) "Проверяем…" else "Проверить обновления")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Подпись ───────────────────────────────────────────────────
            Text(
                text      = "Сделано Вадемом ❤️",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = "v${BuildConfig.VERSION_NAME}",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
