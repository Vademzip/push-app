package com.pushapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.pushapp.model.WorkoutEntry
import com.pushapp.viewmodel.HistoryPeriod
import com.pushapp.viewmodel.WorkoutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

internal data class BarData(val label: String, val sublabel: String = "", val value: Int)

private fun fillMissingDays(entries: List<WorkoutEntry>, days: Int): List<WorkoutEntry> {
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val byDate = entries.associateBy { it.date }
    val today = LocalDate.now()
    return (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong()).format(fmt)
        byDate[date] ?: WorkoutEntry(date = date)
    }
}

/** Генерирует ровно 4 недельных бакета (последние 4 ISO-недели), всегда, даже если данных нет. */
private fun toWeekBarData(entries: List<WorkoutEntry>, getValue: (WorkoutEntry) -> Int): List<BarData> {
    val weekFields = WeekFields.of(Locale("ru"))
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val dayFmt = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))

    val byDate = entries.filter { !it.skipped }.associateBy { it.date }
    val today = LocalDate.now()

    return (3 downTo 0).map { weekOffset ->
        val weekStart = today.with(weekFields.dayOfWeek(), 1).minusWeeks(weekOffset.toLong())
        val weekEnd = weekStart.plusDays(6)

        var total = 0
        var d = weekStart
        while (!d.isAfter(weekEnd)) {
            val entry = byDate[d.format(fmt)]
            if (entry != null) total += getValue(entry)
            d = d.plusDays(1)
        }

        val label = weekStart.format(dayFmt)
        val sublabel = "– ${weekEnd.format(dayFmt)}"
        BarData(label = label, sublabel = sublabel, value = total)
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
        topBar = {
            TopAppBar(
                title = { Text("История") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
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

            val activeEntries = entries.filter { !it.skipped }

            // Итоговые числа — сетка 2×2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TotalCard("💪 Отжимания", activeEntries.sumOf { it.pushups }, Modifier.weight(1f))
                TotalCard("🦵 Приседания", activeEntries.sumOf { it.squats }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TotalCard("🏋️ Подтягивания", activeEntries.sumOf { it.pullups }, Modifier.weight(1f))
                TotalCard("🔥 Пресс", activeEntries.sumOf { it.abs }, Modifier.weight(1f))
            }

            if (entries.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("Нет данных за этот период", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }

            Spacer(Modifier.height(24.dp))

            val primary   = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            val tertiary  = MaterialTheme.colorScheme.tertiary

            if (period == HistoryPeriod.WEEK) {
                val chartEntries = fillMissingDays(entries, days = 7)
                val fmt = DateTimeFormatter.ISO_LOCAL_DATE
                val dayFmt = DateTimeFormatter.ofPattern("dd.MM", Locale("ru"))

                fun toBarData(getValue: (WorkoutEntry) -> Int) = chartEntries.map { e ->
                    val label = try { LocalDate.parse(e.date, fmt).format(dayFmt) } catch (_: Exception) { "" }
                    BarData(label, "", getValue(e))
                }

                ChartSection("Отжимания",    toBarData { it.pushups }, primary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Приседания",   toBarData { it.squats },  secondary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Подтягивания", toBarData { it.pullups }, tertiary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Пресс",        toBarData { it.abs },     primary)
            } else {
                // Месяц — всегда 4 недели
                ChartSection("Отжимания",    toWeekBarData(entries) { it.pushups }, primary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Приседания",   toWeekBarData(entries) { it.squats },  secondary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Подтягивания", toWeekBarData(entries) { it.pullups }, tertiary)
                Spacer(Modifier.height(20.dp))
                ChartSection("Пресс",        toWeekBarData(entries) { it.abs },     primary)
            }
            Spacer(Modifier.height(16.dp))
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
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
internal fun ChartSection(title: String, bars: List<BarData>, color: Color) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    BarChart(bars = bars, barColor = color)
}

@Composable
internal fun BarChart(bars: List<BarData>, barColor: Color) {
    if (bars.isEmpty()) return
    var selectedIndex by remember(bars) { mutableStateOf<Int?>(null) }
    val maxValue = bars.maxOf { it.value }.coerceAtLeast(1).toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { i, bar ->
                val fraction = bar.value / maxValue
                val isSelected = selectedIndex == i

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Значение над столбиком при нажатии
                    if (isSelected) {
                        Text(
                            text = bar.value.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(2.dp))
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }
                    val barHeight = (fraction * 96).dp.coerceAtLeast(2.dp)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (isSelected) barColor else barColor.copy(alpha = 0.75f))
                            .clickable {
                                selectedIndex = if (selectedIndex == i) null else i
                            }
                    )
                }
            }
        }

        // Подписи под столбиками
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = bar.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp
                    )
                    if (bar.sublabel.isNotEmpty()) {
                        Text(
                            text = bar.sublabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
