package com.pushapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.HistoryPeriod
import com.pushapp.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(authViewModel: AuthViewModel, workoutViewModel: WorkoutViewModel) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val allUsers by workoutViewModel.allUsers.collectAsState()
    val myEntries by workoutViewModel.compareMyEntries.collectAsState()
    val otherEntries by workoutViewModel.compareOtherEntries.collectAsState()
    val isLoading by workoutViewModel.isLoading.collectAsState()

    val selectedCompareUserId by workoutViewModel.selectedCompareUserId.collectAsState()
    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.WEEK) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val otherUsers = allUsers.filter { it.uid != currentUser?.uid }
    val selectedUser = otherUsers.find { it.uid == selectedCompareUserId }

    LaunchedEffect(Unit) {
        workoutViewModel.loadAllUsers()
    }

    LaunchedEffect(selectedCompareUserId, selectedPeriod) {
        selectedCompareUserId?.let { workoutViewModel.loadCompare(it, selectedPeriod) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Сравнение") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Выбор пользователя
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedUser?.username ?: "Выбрать пользователя",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Сравнить с") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    if (otherUsers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Другие пользователи не найдены") },
                            onClick = { dropdownExpanded = false },
                            enabled = false
                        )
                    }
                    otherUsers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user.username) },
                            onClick = {
                                workoutViewModel.selectCompareUser(user.uid)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Выбор периода
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    HistoryPeriod.WEEK to "Неделя",
                    HistoryPeriod.MONTH to "Месяц"
                ).forEach { (p, label) ->
                    FilterChip(
                        selected = selectedPeriod == p,
                        onClick = { selectedPeriod = p },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            when {
                selectedCompareUserId == null -> Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Выберите пользователя для сравнения",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                isLoading -> Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                else -> {
                    val myName = currentUser?.username ?: "Я"
                    val otherName = selectedUser?.username ?: "Другой"

                    val myActive    = myEntries.filter { !it.skipped }
                    val otherActive = otherEntries.filter { !it.skipped }

                    val myTotals = listOf(
                        myActive.sumOf { it.pushups },
                        myActive.sumOf { it.squats },
                        myActive.sumOf { it.pullups },
                        myActive.sumOf { it.abs }
                    )
                    val otherTotals = listOf(
                        otherActive.sumOf { it.pushups },
                        otherActive.sumOf { it.squats },
                        otherActive.sumOf { it.pullups },
                        otherActive.sumOf { it.abs }
                    )

                    CompareTable(
                        myName = myName,
                        otherName = otherName,
                        myTotals = myTotals,
                        otherTotals = otherTotals
                    )
                }
            }
        }
    }
}

@Composable
fun CompareTable(
    myName: String,
    otherName: String,
    myTotals: List<Int>,
    otherTotals: List<Int>
) {
    val labels = listOf("Отжимания", "Приседания", "Подтягивания", "Пресс")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Шапка
            Row(Modifier.fillMaxWidth()) {
                Text("", Modifier.weight(2f))
                Text(
                    myName,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    otherName,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            labels.forEachIndexed { i, label ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                CompareRow(label, myTotals[i], otherTotals[i])
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            val mySum = myTotals.sum()
            val otherSum = otherTotals.sum()
            CompareRow("ИТОГО", mySum, otherSum, bold = true)
        }
    }
}

@Composable
fun CompareRow(label: String, myVal: Int, otherVal: Int, bold: Boolean = false) {
    val myWins = myVal > otherVal
    val otherWins = otherVal > myVal
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(2f), style = style)
        Text(
            text = myVal.toString(),
            modifier = Modifier.weight(1f),
            style = style,
            fontWeight = if (myWins) FontWeight.Bold else FontWeight.Normal,
            color = when {
                myWins -> MaterialTheme.colorScheme.primary
                !myWins && otherWins -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = otherVal.toString(),
            modifier = Modifier.weight(1f),
            style = style,
            fontWeight = if (otherWins) FontWeight.Bold else FontWeight.Normal,
            color = when {
                otherWins -> MaterialTheme.colorScheme.primary
                !otherWins && myWins -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
