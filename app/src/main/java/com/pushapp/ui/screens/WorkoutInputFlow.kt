package com.pushapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.launch

private data class WorkoutStep(val label: String, val emoji: String, val maxValue: Int)

private val steps = listOf(
    WorkoutStep("Отжимания", "💪", 500),
    WorkoutStep("Приседания", "🦵", 500),
    WorkoutStep("Подтягивания", "🏋️", 200),
    WorkoutStep("Пресс", "🤸", 500)
)

@Composable
fun WorkoutInputFlow(
    initialPushups: Int = 0,
    initialSquats: Int = 0,
    initialPullups: Int = 0,
    initialAbs: Int = 0,
    initialComment: String = "",
    onSave: (pushups: Int, squats: Int, pullups: Int, abs: Int, comment: String, skipped: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val values = remember { mutableStateListOf(initialPushups, initialSquats, initialPullups, initialAbs) }
    var comment by remember { mutableStateOf(initialComment) }
    var isSkipped by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    val totalSteps = 5 // 4 упражнения + комментарий

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        // ── Шапка ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(totalSteps) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == currentStep) 10.dp else 8.dp)
                            .background(
                                color = if (i <= currentStep)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
            Box(Modifier.size(48.dp))
        }

        // ── Скроллируемый контент ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                label = "step"
            ) { step ->
                if (step < 4) {
                    ExercisePickerStep(
                        step = steps[step],
                        currentValue = values[step],
                        onValueChange = { values[step] = it },
                        isSkipped = isSkipped
                    )
                } else {
                    CommentStep(comment = comment, onCommentChange = { comment = it })
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ── Кнопки ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (currentStep < totalSteps - 1) currentStep++
                    else onSave(values[0], values[1], values[2], values[3], comment, isSkipped)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = if (currentStep < totalSteps - 1) "Далее" else "Сохранить",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isSkipped) {
                TextButton(
                    onClick = {
                        isSkipped = true
                        values[0] = 0; values[1] = 0; values[2] = 0; values[3] = 0
                        currentStep = totalSteps - 1 // сразу к комментарию
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Сегодня не тренировался",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerStep(
    step: WorkoutStep,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    isSkipped: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = step.emoji, fontSize = 72.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = step.label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        if (isSkipped) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Отдых — значения сброшены",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "листай влево/вправо",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(28.dp))
        DrumPicker(
            value = currentValue,
            maxValue = step.maxValue,
            onValueChange = onValueChange,
            enabled = !isSkipped
        )
    }
}

@Composable
private fun CommentStep(comment: String, onCommentChange: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Text(text = "💬", fontSize = 72.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Комментарий",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Необязательно",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = comment,
            onValueChange = { if (it.length <= 200) onCommentChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Как прошла тренировка?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            minLines = 3,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DrumPicker(value: Int, maxValue: Int, onValueChange: (Int) -> Unit, enabled: Boolean = true) {
    val itemWidth = 100.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val sidePadding = (screenWidth - itemWidth) / 2

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && enabled) {
            val center = listState.firstVisibleItemIndex +
                    if (listState.firstVisibleItemScrollOffset > 0) 1 else 0
            val snapped = center.coerceIn(0, maxValue)
            if (snapped != value) onValueChange(snapped)
        }
    }

    LaunchedEffect(value) {
        if (abs(listState.firstVisibleItemIndex - value) > 1) {
            scope.launch { listState.scrollToItem(value) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(itemWidth + 16.dp)
                .height(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
        )
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = sidePadding),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!enabled) Modifier else Modifier),
            userScrollEnabled = enabled
        ) {
            items(maxValue + 1) { num ->
                val dist = abs(
                    num - (listState.firstVisibleItemIndex +
                            if (listState.firstVisibleItemScrollOffset > (itemWidth / 2).value) 1 else 0)
                )
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(160.dp)
                        .alpha(when (dist) { 0 -> 1f; 1 -> 0.45f; 2 -> 0.2f; else -> 0.05f }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = num.toString(),
                        fontSize = when (dist) { 0 -> 72.sp; 1 -> 44.sp; 2 -> 28.sp; else -> 18.sp },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (dist == 0) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
