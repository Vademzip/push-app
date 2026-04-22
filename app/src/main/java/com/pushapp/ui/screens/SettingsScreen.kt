package com.pushapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.BuildConfig
import com.pushapp.notification.NotificationHelper
import com.pushapp.util.AppUpdater
import com.pushapp.util.ThemePrefs
import com.pushapp.util.UpdateInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onAccentChanged: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val (savedHour, savedMinute) = remember { NotificationHelper.getNotificationTime(context) }
    val timePickerState = rememberTimePickerState(
        initialHour   = savedHour,
        initialMinute = savedMinute,
        is24Hour      = true
    )
    var showTimePicker   by remember { mutableStateOf(false) }
    var displayHour      by remember { mutableStateOf(savedHour) }
    var displayMinute    by remember { mutableStateOf(savedMinute) }

    var selectedAccent   by remember { mutableStateOf(ThemePrefs.getKey(context)) }

    // Состояние проверки и скачивания обновлений
    var updateChecking   by remember { mutableStateOf(false) }
    var updateInfo       by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateChecked    by remember { mutableStateOf(false) }
    var downloadId       by remember { mutableStateOf<Long?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }

    // Опрашиваем прогресс скачивания
    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        while (true) {
            val progress = AppUpdater.getDownloadProgress(context, id)
            downloadProgress = progress
            if (progress == null) break
            kotlinx.coroutines.delay(300)
        }
    }

    // Диалог выбора времени
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    NotificationHelper.saveNotificationTime(context, timePickerState.hour, timePickerState.minute)
                    NotificationHelper.scheduleDailyReminder(context)
                    displayHour   = timePickerState.hour
                    displayMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            },
            title = { Text("Время напоминания") },
            text  = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Уведомления ───────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Text(
                text     = "Уведомления",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Время напоминания", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text(
                            "%02d:%02d".format(displayHour, displayMinute),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Цвет акцента ──────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text     = "Внешний вид",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Цвет акцента", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemePrefs.options.forEach { option ->
                            val isSelected = option.key == selectedAccent
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                    .clickable {
                                        selectedAccent = option.key
                                        ThemePrefs.setKey(context, option.key)
                                        onAccentChanged(option.key)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text("✓", color = MaterialTheme.colorScheme.background,
                                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = ThemePrefs.options.find { it.key == selectedAccent }?.label ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Обновление ────────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text     = "Обновление",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Текущая версия", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    when {
                        updateChecked && updateInfo == null -> Text(
                            text  = "Актуальная версия, обновлений нет ✓",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        updateInfo != null -> {
                            Text(
                                text       = "Доступна версия ${updateInfo!!.versionName}!",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
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
                            when {
                                downloadProgress != null -> {
                                    Text(
                                        text = "Скачивание… ${(downloadProgress!! * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress!! },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                downloadId != null && downloadProgress == null -> {
                                    Text(
                                        text = "Скачано! Открываем установщик…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                else -> {
                                    Button(
                                        onClick = {
                                            downloadId = AppUpdater.downloadAndInstall(context, updateInfo!!)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(50.dp)
                                    ) { Text("Скачать и установить") }
                                }
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

            Spacer(Modifier.height(32.dp))

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
