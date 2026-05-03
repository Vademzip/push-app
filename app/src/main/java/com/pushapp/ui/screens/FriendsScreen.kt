package com.pushapp.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.model.FriendEntry
import com.pushapp.model.User
import com.pushapp.viewmodel.FriendViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(friendViewModel: FriendViewModel) {
    val friends          by friendViewModel.friends.collectAsState()
    val incomingRequests by friendViewModel.incomingRequests.collectAsState()
    val outgoingRequests by friendViewModel.outgoingRequests.collectAsState()
    val searchResults    by friendViewModel.searchResults.collectAsState()
    val isSearching      by friendViewModel.isSearching.collectAsState()

    var query          by remember { mutableStateOf("") }
    var selectedFriend by remember { mutableStateOf<FriendEntry?>(null) }
    var pokeTarget     by remember { mutableStateOf<FriendEntry?>(null) }
    val context        = LocalContext.current
    val showResults    = query.length >= 2

    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(400)
            friendViewModel.search(query)
        } else {
            friendViewModel.clearSearch()
        }
    }

    val friendUids   = remember(friends)          { friends.map { it.uid }.toSet() }
    val incomingUids = remember(incomingRequests) { incomingRequests.map { it.uid }.toSet() }
    val outgoingUids = remember(outgoingRequests) { outgoingRequests.map { it.uid }.toSet() }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title  = { Text("Друзья", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    placeholder   = { Text("Найти пользователя") },
                    leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon  = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(50)
                )

                if (showResults) {
                    when {
                        isSearching -> {
                            Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        searchResults.isEmpty() -> {
                            Box(Modifier.fillMaxWidth().padding(top = 48.dp), Alignment.Center) {
                                Text("Никого не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> {
                            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(searchResults, key = { it.uid }) { user ->
                                    val status = when {
                                        user.uid in friendUids   -> RelationStatus.FRIENDS
                                        user.uid in incomingUids -> RelationStatus.INCOMING
                                        user.uid in outgoingUids -> RelationStatus.OUTGOING
                                        else                     -> RelationStatus.NONE
                                    }
                                    SearchResultRow(user, status, friendViewModel)
                                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        if (incomingRequests.isNotEmpty()) {
                            item { SectionLabel("Заявки в друзья") }
                            items(incomingRequests, key = { it.uid }) { entry ->
                                UserRow(username = entry.username, displayName = entry.displayName) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { friendViewModel.acceptRequest(entry.uid) },
                                            shape   = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) { Text("Принять") }
                                        OutlinedButton(
                                            onClick = { friendViewModel.declineOrRemove(entry.uid) },
                                            shape   = RoundedCornerShape(50),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) { Text("Отклонить") }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                            }
                        }

                        if (outgoingRequests.isNotEmpty()) {
                            item { SectionLabel("Отправленные заявки") }
                            items(outgoingRequests, key = { it.uid }) { entry ->
                                UserRow(username = entry.username, displayName = entry.displayName) {
                                    OutlinedButton(
                                        onClick = { friendViewModel.declineOrRemove(entry.uid) },
                                        shape   = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) { Text("Отменить") }
                                }
                                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                            }
                        }

                        if (friends.isNotEmpty()) {
                            item { SectionLabel("Друзья · ${friends.size}") }
                            items(friends, key = { it.uid }) { entry ->
                                UserRow(
                                    username    = entry.username,
                                    displayName = entry.displayName,
                                    onClick     = { selectedFriend = entry }
                                ) {}
                                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                            }
                        }

                        if (friends.isEmpty() && incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                            item { EmptyFriendsState() }
                        }
                    }
                }
            }
        }

        // Флоу «Пнуть»
        AnimatedVisibility(
            visible = pokeTarget != null,
            enter   = slideInVertically { it } + fadeIn(),
            exit    = slideOutVertically { it } + fadeOut()
        ) {
            pokeTarget?.let { target ->
                PokeFlow(
                    friend    = target,
                    context   = context,
                    onDismiss = { pokeTarget = null }
                )
            }
        }
    }

    // Шторка друга
    selectedFriend?.let { friend ->
        val friendLabel = friend.displayName.ifBlank { friend.username }
        ModalBottomSheet(
            onDismissRequest = { selectedFriend = null },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = friendLabel.firstOrNull()?.uppercase() ?: "?",
                        fontSize   = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = friendLabel, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (friend.displayName.isNotBlank()) {
                        Text(
                            text  = "@${friend.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { pokeTarget = friend; selectedFriend = null },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(50.dp)
                ) {
                    Text("👊  Пнуть", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = {
                        friendViewModel.declineOrRemove(friend.uid)
                        selectedFriend = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Удалить из друзей", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PokeFlow(
    friend: FriendEntry,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    val friendLabel = friend.displayName.ifBlank { friend.username }
    val messages = remember(friendLabel) {
        listOf(
            "$friendLabel, хватит лежать на диване! 💪\nДавай тренируйся, открывай PushApp!",
            "$friendLabel, ты вообще помнишь, что такое тренировка? 😅\nПора исправляться!",
            "Эй, $friendLabel! 🏋️ Пока ты читаешь это — я уже жму лёжа. Не отставай!",
            "$friendLabel, одно отжимание — и уже не день ноля. Вставай! 🙄"
        )
    }
    var selected by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Шапка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
            Box(Modifier.size(48.dp))
        }

        // Контент
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(8.dp))
            Text("👊", fontSize = 72.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "Пнуть $friendLabel",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "Выбери сообщение",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            messages.forEachIndexed { index, msg ->
                val isSelected = index == selected
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { selected = index },
                    shape  = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text     = msg,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Кнопка
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, messages[selected])
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                },
                modifier  = Modifier.fillMaxWidth().height(58.dp),
                shape     = RoundedCornerShape(50.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Отправить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private enum class RelationStatus { NONE, OUTGOING, INCOMING, FRIENDS }

@Composable
private fun SearchResultRow(user: User, status: RelationStatus, vm: FriendViewModel) {
    UserRow(username = user.username, displayName = user.displayName) {
        when (status) {
            RelationStatus.NONE -> Button(
                onClick = { vm.sendRequest(user.uid, user.username, user.displayName) },
                shape   = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Добавить") }

            RelationStatus.OUTGOING -> OutlinedButton(
                onClick = { vm.declineOrRemove(user.uid) },
                shape   = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Отменить") }

            RelationStatus.INCOMING -> Button(
                onClick = { vm.acceptRequest(user.uid) },
                shape   = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Принять") }

            RelationStatus.FRIENDS -> Text(
                "✓ Друг",
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun UserRow(
    username: String,
    displayName: String = "",
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val label = displayName.ifBlank { username }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = label.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                color      = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (displayName.isNotBlank()) {
                Text(
                    text  = "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun EmptyFriendsState() {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("👥", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text("Пока нет друзей", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Найди друга по имени выше",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
