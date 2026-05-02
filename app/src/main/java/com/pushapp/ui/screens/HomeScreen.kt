package com.pushapp.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.model.WorkoutEntry
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(authViewModel: AuthViewModel, workoutViewModel: WorkoutViewModel, onInputActive: (Boolean) -> Unit = {}) {
    val currentUser    by authViewModel.currentUser.collectAsState()
    val todayWorkout   by workoutViewModel.todayWorkout.collectAsState()
    val isTodayLoading by workoutViewModel.isTodayLoading.collectAsState()
    val saveSuccess    by workoutViewModel.saveSuccess.collectAsState()
    val weeklyInsight  by workoutViewModel.weeklyInsight.collectAsState()
    var showInputFlow  by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val dateStr = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru")))

    LaunchedEffect(Unit) { workoutViewModel.loadTodayWorkout() }
    LaunchedEffect(showInputFlow) { onInputActive(showInputFlow) }

    LaunchedEffect(saveSuccess) {
        when (saveSuccess) {
            true -> { snackbarHostState.showSnackbar("Сохранено!"); workoutViewModel.resetSaveState() }
            false -> { snackbarHostState.showSnackbar("Ошибка, проверь интернет"); workoutViewModel.resetSaveState() }
            null -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Основной экран ──────────────────────────────────────────────────
        Scaffold(
            floatingActionButton = {
                if (!showInputFlow) {
                    ExtendedFloatingActionButton(
                        onClick = { showInputFlow = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(if (todayWorkout != null) "Обновить" else "Добавить") },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Text(text = "Привет, ${currentUser?.username ?: ""}!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = dateStr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                when {
                    isTodayLoading                -> TodaySkeleton()
                    todayWorkout?.skipped == true -> RestDayCard(entry = todayWorkout!!)
                    todayWorkout != null          -> TodayCard(entry = todayWorkout!!)
                    else                          -> EmptyToday()
                }

                if (weeklyInsight != null) {
                    Spacer(Modifier.height(16.dp))
                    WeeklyInsightCard(
                        text = weeklyInsight!!,
                        onDismiss = { workoutViewModel.dismissWeeklyInsight() }
                    )
                }
            }
        }

        // ── Флоу добавления — поверх всего, без Dialog ─────────────────────
        if (showInputFlow) {
            WorkoutInputFlow(
                initialPushups = todayWorkout?.pushups ?: 0,
                initialSquats = todayWorkout?.squats ?: 0,
                initialPullups = todayWorkout?.pullups ?: 0,
                initialAbs = todayWorkout?.abs ?: 0,
                initialComment = todayWorkout?.comment ?: "",
                onSave = { pushups, squats, pullups, abs, comment, skipped ->
                    workoutViewModel.saveWorkout(
                        pushups, squats, pullups, abs,
                        currentUser?.username ?: "", comment, skipped
                    )
                    showInputFlow = false
                },
                onDismiss = { showInputFlow = false }
            )
        }
    }
}

@Composable
private fun TodayCard(entry: WorkoutEntry) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Сегодня",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = {
                    val lines = buildList {
                        if (entry.pushups > 0) add("💪 Отжимания: ${entry.pushups}")
                        if (entry.squats  > 0) add("🦵 Приседания: ${entry.squats}")
                        if (entry.pullups > 0) add("🏋️ Подтягивания: ${entry.pullups}")
                        if (entry.abs     > 0) add("🤸 Пресс: ${entry.abs}")
                    }
                    val stats = if (lines.isEmpty()) "Тренировка завершена!" else lines.joinToString("\n")
                    val comment = if (entry.comment.isNotBlank()) "\n\n\"${entry.comment}\"" else ""
                    val msg = "Сегодня я сделал:\n$stats$comment\n\nТренируйся вместе со мной в PushApp!\npushapp://open"
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }, null
                        )
                    )
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Поделиться",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BigStatCard("💪", entry.pushups, "Отжимания", Modifier.weight(1f))
            BigStatCard("🦵", entry.squats, "Приседания", Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BigStatCard("🏋️", entry.pullups, "Подтягивания", Modifier.weight(1f))
            BigStatCard("🤸", entry.abs, "Пресс", Modifier.weight(1f))
        }
        if (entry.comment.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "\"${entry.comment}\"",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RestDayCard(entry: WorkoutEntry) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Сегодня",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = {
                    val comment = if (entry.comment.isNotBlank()) "\n\n\"${entry.comment}\"" else ""
                    val msg = "Сегодня у меня день отдыха 😴$comment\n\nСледи за моими тренировками в PushApp!\npushapp://open"
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }, null
                        )
                    )
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Поделиться",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "😴", fontSize = 56.sp)
                Text(
                    text = "День отдыха",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (entry.comment.isNotBlank()) {
                    Text(
                        text = "\"${entry.comment}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BigStatCard(emoji: String, value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = value.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WeeklyInsightCard(text: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text     = text,
                modifier = Modifier.weight(1f),
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Скрыть",
                    tint   = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TodaySkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmerX by transition.animateFloat(
        initialValue = -300f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surfaceVariant,
        ),
        start = Offset(shimmerX, 0f),
        end   = Offset(shimmerX + 300f, 200f)
    )

    @Composable
    fun SkeletonBox(modifier: Modifier) {
        Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(shimmerBrush))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "Сегодня" label
        SkeletonBox(Modifier.width(80.dp).height(16.dp))

        // Сетка 2×2
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }

        // Плейсхолдер комментария
        SkeletonBox(Modifier.fillMaxWidth().height(56.dp))
    }
}

@Composable
private fun EmptyToday() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "🏃", fontSize = 80.sp)
        Text(
            text = "Ещё не тренировался",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Нажми кнопку ниже чтобы добавить результаты",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
