package com.pushapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.WorkoutViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(authViewModel: AuthViewModel, workoutViewModel: WorkoutViewModel) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allUsers by workoutViewModel.allUsers.collectAsState()
    val myDates by workoutViewModel.calendarMyDates.collectAsState()
    val friendDates by workoutViewModel.calendarFriendDates.collectAsState()
    val selectedFriend by workoutViewModel.selectedCalendarFriend.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val otherUsers = allUsers.filter { it.uid != currentUser?.uid }

    LaunchedEffect(Unit) {
        workoutViewModel.loadAllUsers()
    }

    LaunchedEffect(currentMonth, selectedFriend) {
        workoutViewModel.loadCalendarMonth(currentMonth)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Календарь") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Выбор друга
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedFriend?.username ?: "Выбрать друга (жёлтые кружки)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Друг для сравнения") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Никто") },
                        onClick = {
                            workoutViewModel.selectCalendarFriend(null)
                            dropdownExpanded = false
                        }
                    )
                    otherUsers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user.username) },
                            onClick = {
                                workoutViewModel.selectCalendarFriend(user)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Легенда
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = Color(0xFF4CAF50), label = "Я тренировался")
                if (selectedFriend != null) {
                    LegendItem(color = Color(0xFFFFC107), label = selectedFriend!!.username)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Навигация по месяцам
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Назад")
                }
                Text(
                    text = currentMonth.month
                        .getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
                        .replaceFirstChar { it.uppercase() } + " ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = {
                        if (currentMonth < YearMonth.now()) {
                            currentMonth = currentMonth.plusMonths(1)
                        }
                    },
                    enabled = currentMonth < YearMonth.now()
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Вперёд")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Дни недели
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Сетка дней
            MonthGrid(
                yearMonth = currentMonth,
                myDates = myDates,
                friendDates = friendDates
            )
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    myDates: Set<String>,
    friendDates: Set<String>
) {
    val today = LocalDate.now()
    val firstDay = yearMonth.atDay(1)
    // Понедельник = 1, воскресенье = 7 → сдвиг для сетки
    val startOffset = (firstDay.dayOfWeek.value - 1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    val date = if (dayNumber in 1..daysInMonth) yearMonth.atDay(dayNumber) else null
                    val dateStr = date?.toString() ?: ""

                    CalendarCell(
                        date = date,
                        isToday = date == today,
                        hasMyWorkout = dateStr.isNotEmpty() && myDates.contains(dateStr),
                        hasFriendWorkout = dateStr.isNotEmpty() && friendDates.contains(dateStr),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate?,
    isToday: Boolean,
    hasMyWorkout: Boolean,
    hasFriendWorkout: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
    ) {
        if (date != null) {
            // Фон для сегодня
            if (isToday) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }

            // Номер дня
            Text(
                text = date.dayOfMonth.toString(),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            // Жёлтый кружок (друг) — левый верхний угол
            if (hasFriendWorkout) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopStart)
                        .padding(1.dp)
                        .background(Color(0xFFFFC107), CircleShape)
                )
            }

            // Зелёный кружок (я) — правый верхний угол
            if (hasMyWorkout) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(1.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
