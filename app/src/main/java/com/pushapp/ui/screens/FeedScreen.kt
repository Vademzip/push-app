package com.pushapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun FeedScreen(authViewModel: AuthViewModel, workoutViewModel: WorkoutViewModel) {
    val feed by workoutViewModel.feed.collectAsState()
    val isLoading by workoutViewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) { workoutViewModel.loadFeed() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Лента", fontSize = 28.sp, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            feed.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😶", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Пока нет записей", style = MaterialTheme.typography.titleMedium)
                }
            }
            else -> {
                val grouped = remember(feed) {
                    feed.groupBy { it.date }
                        .entries
                        .sortedByDescending { it.key }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    grouped.forEach { (date, dayEntries) ->
                        item(key = "header_$date") {
                            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val yesterdayStr = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val headerLabel = when (date) {
                                todayStr -> "Сегодня"
                                yesterdayStr -> "Вчера"
                                else -> try {
                                    LocalDate.parse(date).format(
                                        DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
                                    )
                                } catch (e: Exception) { date }
                            }
                            Text(
                                text = headerLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(dayEntries, key = { it.id }) { entry ->
                            FeedCard(
                                entry = entry,
                                currentUsername = currentUser?.username ?: "",
                                workoutViewModel = workoutViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedCard(
    entry: WorkoutEntry,
    currentUsername: String,
    workoutViewModel: WorkoutViewModel
) {
    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val isToday = entry.date == todayStr
    val dateLabel = if (isToday) "сегодня" else try {
        LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern("d MMM", Locale("ru")))
    } catch (e: Exception) { entry.date }

    val allComments by workoutViewModel.comments.collectAsState()
    val comments = allComments[entry.id]
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(showComments) {
        if (showComments && comments == null) workoutViewModel.loadComments(entry.id)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = entry.username, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (entry.pushups > 0) FeedStat("💪", entry.pushups, "ауж.", Modifier.weight(1f))
                if (entry.squats > 0) FeedStat("🦵", entry.squats, "прис.", Modifier.weight(1f))
                if (entry.pullups > 0) FeedStat("🏋️", entry.pullups, "подт.", Modifier.weight(1f))
            }
            if (entry.comment.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "\"${entry.comment}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Комментарии ─────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = { showComments = !showComments },
                contentPadding = PaddingValues(0.dp)
            ) {
                val label = when {
                    !showComments -> "Комментарии ${if (!comments.isNullOrEmpty()) "(${comments.size})" else ""}"
                    else -> "Скрыть"
                }
                Text(label, style = MaterialTheme.typography.labelMedium)
            }

            if (showComments) {
                Spacer(Modifier.height(8.dp))
                if (comments == null) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    comments.forEach { c ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                text = c.username,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(text = c.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (comments.isEmpty()) {
                        Text(
                            text = "Нет комментариев",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Написать комментарий…", style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val text = commentText.trim()
                                if (text.isNotEmpty() && currentUsername.isNotEmpty()) {
                                    workoutViewModel.addComment(entry.id, text, currentUsername)
                                    commentText = ""
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Отправить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedStat(emoji: String, value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Text(text = value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun Badge(label: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
