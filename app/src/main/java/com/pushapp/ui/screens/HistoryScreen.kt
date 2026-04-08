package com.pushapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pushapp.model.WorkoutEntry
import com.pushapp.viewmodel.HistoryPeriod
import com.pushapp.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Заполняет пропущенные дни нулевыми записями, чтобы на графике было ровно [days] столбцов. */
private fun fillMissingDays(entries: List<WorkoutEntry>, days: Int): List<WorkoutEntry> {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val byDate = entries.associateBy { it.date }
    val today = LocalDate.now()
    return (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong()).format(fmt)
        byDate[date] ?: WorkoutEntry(date = date)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(workoutViewModel: WorkoutViewModel) {
    val entries by workoutViewModel.historyEntries.collectAsState()
    val isLoading by workoutViewModel.isLoading.collectAsState()
    var period by remember { mutableStateOf(HistoryPeriod.WEEK) }

    LaunchedEffect(period) {
        workoutViewModel.loadHistory(period)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("История") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Выбор периода
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    HistoryPeriod.DAY to "День",
                    HistoryPeriod.WEEK to "Неделя",
                    HistoryPeriod.MONTH to "Месяц"
                ).forEach { (p, label) ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Нет данных за этот период", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            // Итоговые числа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TotalCard("Отжимания", entries.sumOf { it.pushups }, Modifier.weight(1f))
                TotalCard("Приседания", entries.sumOf { it.squats }, Modifier.weight(1f))
                TotalCard("Подтягивания", entries.sumOf { it.pullups }, Modifier.weight(1f))
            }

            if (period == HistoryPeriod.DAY) {
                Spacer(Modifier.height(20.dp))
                entries.firstOrNull()?.let { DayDetail(it) }
            } else {
                Spacer(Modifier.height(24.dp))
                val primary   = MaterialTheme.colorScheme.primary
                val secondary = MaterialTheme.colorScheme.secondary
                val tertiary  = MaterialTheme.colorScheme.tertiary

                val chartEntries = if (period == HistoryPeriod.WEEK) {
                    fillMissingDays(entries, days = 7)
                } else {
                    entries
                }

                ChartSection("Отжимания",   chartEntries, { it.pushups }, primary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Приседания",  chartEntries, { it.squats },  secondary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Подтягивания", chartEntries, { it.pullups }, tertiary)
            }
        }
    }
}

@Composable
fun TotalCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ChartSection(
    title: String,
    entries: List<WorkoutEntry>,
    getValue: (WorkoutEntry) -> Int,
    color: Color
) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    BarChart(entries = entries, getValue = getValue, barColor = color)
}

@Composable
fun BarChart(entries: List<WorkoutEntry>, getValue: (WorkoutEntry) -> Int, barColor: Color) {
    val maxValue = entries.maxOf { getValue(it) }.coerceAtLeast(1).toFloat()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
        val count = entries.size
        val slotWidth = size.width / count
        val barWidth = slotWidth * 0.55f
        val maxBarH = size.height - 24.dp.toPx()

        entries.forEachIndexed { i, entry ->
            val v = getValue(entry)
            val barH = (v / maxValue) * maxBarH
            val x = i * slotWidth + (slotWidth - barWidth) / 2
            val y = maxBarH - barH

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH.coerceAtLeast(2.dp.toPx())),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }

    // Подписи дат под графиком
    if (entries.size <= 10) {
        Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { entry ->
                val label = try {
                    LocalDate.parse(entry.date)
                        .format(DateTimeFormatter.ofPattern("d/M", Locale("ru")))
                } catch (e: Exception) { "" }
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DayDetail(entry: WorkoutEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Детали за день", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            DetailRow("Отжимания", entry.pushups)
            DetailRow("Приседания", entry.squats)
            DetailRow("Подтягивания", entry.pullups)
        }
    }
}

@Composable
fun DetailRow(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text("$value раз", fontWeight = FontWeight.SemiBold)
    }
}
