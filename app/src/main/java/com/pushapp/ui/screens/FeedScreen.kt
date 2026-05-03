package com.pushapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.model.WorkoutEntry
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.FeedSocialStats
import com.pushapp.viewmodel.FriendViewModel
import com.pushapp.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    authViewModel: AuthViewModel,
    workoutViewModel: WorkoutViewModel,
    friendViewModel: FriendViewModel
) {
    val feed        by workoutViewModel.feed.collectAsState()
    val isLoading   by workoutViewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val friends     by friendViewModel.friends.collectAsState()
    val socialStats by workoutViewModel.socialStats.collectAsState()

    val friendsInitialized by friendViewModel.friendsInitialized.collectAsState()
    val friendUids = remember(friends) { friends.map { it.uid } }
    val triggerKey = if (friendsInitialized) friendUids else null

    LaunchedEffect(triggerKey) {
        triggerKey?.let {
            workoutViewModel.loadFriendsFeed(it)
            workoutViewModel.loadSocialStats(it)
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            workoutViewModel.loadFriendsFeed(friendUids, forceRefresh = true)
            workoutViewModel.loadSocialStats(friendUids, forceRefresh = true)
            pullToRefreshState.endRefresh()
        }
    }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }

    // userId → отображаемое имя
    val nameMap = remember(currentUser, friends) {
        buildMap {
            currentUser?.let { put(it.uid, it.displayName.ifBlank { it.username }) }
            friends.forEach { put(it.uid, it.displayName.ifBlank { it.username }) }
        }
    }
    // Все участники группы (я + друзья) в порядке: сначала я
    val allParticipants = remember(currentUser, friends) {
        buildList {
            currentUser?.let { add(it.uid to it.displayName.ifBlank { it.username }) }
            friends.forEach { add(it.uid to it.displayName.ifBlank { it.username }) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Лента", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            val grouped = remember(feed) {
                feed.groupBy { it.date }.entries.sortedByDescending { it.key }
            }

            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Социальные виджеты ────────────────────────────────────────
                val stats = socialStats
                if (stats != null && allParticipants.size > 1) {
                    item(key = "today_participants") {
                        TodayParticipantsCard(
                            participants = allParticipants,
                            todayUids    = stats.todayUids
                        )
                    }
                    item(key = "week_recap") {
                        WeekRecapCard(
                            stats     = stats,
                            nameMap   = nameMap,
                            groupSize = allParticipants.size
                        )
                    }
                }

                // ── Лента тренировок ──────────────────────────────────────────
                when {
                    isLoading && !pullToRefreshState.isRefreshing -> item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    feed.isEmpty() -> item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("😶", fontSize = 64.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Пока нет записей", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    else -> {
                        grouped.forEach { (date, dayEntries) ->
                            item(key = "header_$date") {
                                val todayS     = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val yesterdayS = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val label = when (date) {
                                    todayS     -> "Сегодня"
                                    yesterdayS -> "Вчера"
                                    else       -> try {
                                        LocalDate.parse(date).format(
                                            DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
                                        )
                                    } catch (e: Exception) { date }
                                }
                                Text(
                                    text     = label,
                                    style    = MaterialTheme.typography.labelLarge,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(dayEntries, key = { it.id }) { entry ->
                                val streak = socialStats?.streaks?.get(entry.userId) ?: 0
                                val pb     = socialStats?.personalBests?.get(entry.userId)
                                val prExercises = if (entry.date == todayStr && !entry.skipped && pb != null) {
                                    buildSet {
                                        if (entry.pushups > 0 && (pb["pushups"] ?: 0) > 0 && entry.pushups > (pb["pushups"] ?: 0)) add("pushups")
                                        if (entry.squats  > 0 && (pb["squats"]  ?: 0) > 0 && entry.squats  > (pb["squats"]  ?: 0)) add("squats")
                                        if (entry.pullups > 0 && (pb["pullups"] ?: 0) > 0 && entry.pullups > (pb["pullups"] ?: 0)) add("pullups")
                                        if (entry.abs     > 0 && (pb["abs"]     ?: 0) > 0 && entry.abs     > (pb["abs"]     ?: 0)) add("abs")
                                    }
                                } else emptySet()

                                FeedCard(
                                    entry           = entry,
                                    currentUsername = currentUser?.username ?: "",
                                    workoutViewModel = workoutViewModel,
                                    streak          = streak,
                                    prExercises     = prExercises
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state    = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// ── Кто тренировался сегодня ──────────────────────────────────────────────────

@Composable
private fun TodayParticipantsCard(
    participants: List<Pair<String, String>>,
    todayUids: Set<String>
) {
    val trained = participants.count { it.first in todayUids }
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier       = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Сегодня", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "$trained из ${participants.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier              = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                participants.forEach { (uid, name) ->
                    val active = uid in todayUids
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.width(52.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = name.firstOrNull()?.uppercase() ?: "?",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 18.sp,
                                color      = if (active) MaterialTheme.colorScheme.onPrimary
                                             else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text     = name.split(" ").first(),
                            style    = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color    = if (active) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ── Рекап недели + лидерборд ──────────────────────────────────────────────────

@Composable
private fun weightedScore(pushups: Int, squats: Int, pullups: Int, abs: Int): Int =
    (pushups * 1.0 + squats * 0.5 + pullups * 1.5 + abs * 0.75).toInt()

@Composable
private fun WeekRecapCard(
    stats: FeedSocialStats,
    nameMap: Map<String, String>,
    groupSize: Int
) {
    val weekEntries = stats.weekEntries
    val totalPushups = weekEntries.sumOf { it.pushups }
    val totalSquats  = weekEntries.sumOf { it.squats }
    val totalPullups = weekEntries.sumOf { it.pullups }
    val totalAbs     = weekEntries.sumOf { it.abs }

    val workoutCount  = weekEntries.map { it.userId to it.date }.toSet().size
    val possibleCount = groupSize * 7

    val leaderboard = remember(weekEntries, nameMap) {
        weekEntries
            .groupBy { it.userId }
            .map { (uid, entries) ->
                val score = (
                    entries.sumOf { it.pushups } * 1.0 +
                    entries.sumOf { it.squats }  * 0.5 +
                    entries.sumOf { it.pullups } * 1.5 +
                    entries.sumOf { it.abs }     * 0.75
                ).toInt()
                (nameMap[uid] ?: "?") to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)
    }

    val medals = listOf("🥇", "🥈", "🥉")
    val fmt    = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    val weekStart = try { LocalDate.now().minusDays(6).format(fmt) } catch (e: Exception) { "" }
    val weekEnd   = try { LocalDate.now().format(fmt) }               catch (e: Exception) { "" }

    var showScoreInfo by remember { mutableStateOf(false) }

    if (showScoreInfo) {
        AlertDialog(
            onDismissRequest = { showScoreInfo = false },
            title = { Text("Как считаются очки") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Каждое упражнение весит по-своему:")
                    Spacer(Modifier.height(2.dp))
                    ScoreRow("💪", "Отжимания",   "× 1.0")
                    ScoreRow("🦵", "Приседания",  "× 0.5")
                    ScoreRow("🏋️", "Подтягивания","× 1.5")
                    ScoreRow("🤸", "Пресс",       "× 0.75")
                }
            },
            confirmButton = {
                TextButton(onClick = { showScoreInfo = false }) { Text("Понятно") }
            }
        )
    }

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Неделя", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "$weekStart – $weekEnd",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (totalPushups > 0) RecapStat("💪", totalPushups, "отж.",  Modifier.weight(1f))
                if (totalSquats  > 0) RecapStat("🦵", totalSquats,  "прис.", Modifier.weight(1f))
                if (totalPullups > 0) RecapStat("🏋️", totalPullups, "подт.", Modifier.weight(1f))
                if (totalAbs     > 0) RecapStat("🤸", totalAbs,     "пресс", Modifier.weight(1f))
                if (totalPushups == 0 && totalSquats == 0 && totalPullups == 0 && totalAbs == 0) {
                    Text(
                        "Пока нет тренировок за эту неделю",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text  = "$workoutCount тренировок из $possibleCount возможных",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (leaderboard.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text  = "Лидерборд",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick  = { showScoreInfo = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Как считаются очки",
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                leaderboard.forEachIndexed { i, (name, score) ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(medals[i], fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = name,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                            modifier   = Modifier.weight(1f)
                        )
                        Text(
                            text  = "${formatNumber(score)} очк.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(emoji: String, label: String, coeff: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(coeff, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun RecapStat(emoji: String, value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(formatNumber(value), fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatNumber(n: Int): String =
    if (n >= 1000) "${n / 1000} ${"%03d".format(n % 1000)}" else n.toString()

// ── Карточка тренировки ───────────────────────────────────────────────────────

@Composable
fun FeedCard(
    entry: WorkoutEntry,
    currentUsername: String,
    workoutViewModel: WorkoutViewModel,
    streak: Int = 0,
    prExercises: Set<String> = emptySet()
) {
    val todayStr  = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val isToday   = entry.date == todayStr
    val dateLabel = if (isToday) "сегодня" else try {
        LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern("d MMM", Locale("ru")))
    } catch (e: Exception) { entry.date }

    val allComments  by workoutViewModel.comments.collectAsState()
    val comments      = allComments[entry.id]
    var showComments  by remember { mutableStateOf(false) }
    var commentText   by remember { mutableStateOf("") }

    LaunchedEffect(showComments) {
        if (showComments && comments == null) workoutViewModel.loadComments(entry.id)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = entry.username, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (streak >= 2) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text     = "🔥 $streak",
                                style    = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color    = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Text(
                    text  = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))

            if (entry.skipped) {
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("😴", fontSize = 22.sp)
                        Text(
                            text  = "День отдыха",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (entry.pushups > 0) FeedStat("💪", entry.pushups, "отж.",  Modifier.weight(1f), "pushups" in prExercises)
                    if (entry.squats  > 0) FeedStat("🦵", entry.squats,  "прис.", Modifier.weight(1f), "squats"  in prExercises)
                    if (entry.pullups > 0) FeedStat("🏋️", entry.pullups, "подт.", Modifier.weight(1f), "pullups" in prExercises)
                    if (entry.abs     > 0) FeedStat("🤸", entry.abs,     "пресс", Modifier.weight(1f), "abs"     in prExercises)
                }
            }

            if (entry.comment.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = "\"${entry.comment}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick        = { showComments = !showComments },
                contentPadding = PaddingValues(0.dp)
            ) {
                val label = when {
                    !showComments -> "Комментарии ${if (!comments.isNullOrEmpty()) "(${comments.size})" else ""}"
                    else          -> "Скрыть"
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
                                text       = c.username,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(end = 6.dp)
                            )
                            Text(text = c.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (comments.isEmpty()) {
                        Text(
                            text  = "Нет комментариев",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value         = commentText,
                            onValueChange = { commentText = it },
                            placeholder   = { Text("Написать комментарий…", style = MaterialTheme.typography.bodySmall) },
                            modifier      = Modifier.weight(1f),
                            singleLine    = true,
                            shape         = RoundedCornerShape(16.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                cursorColor          = MaterialTheme.colorScheme.primary,
                            )
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
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedStat(
    emoji: String,
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    isPR: Boolean = false
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = emoji, fontSize = 28.sp)
                Text(text = value.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isPR) {
            Text(
                text     = "🏆",
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
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
            text     = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
