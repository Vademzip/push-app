package com.pushapp.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pushapp.ui.screens.*
import com.pushapp.ui.theme.*
import com.pushapp.viewmodel.AuthState
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.FriendViewModel
import com.pushapp.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home     : Screen("home",     Icons.Default.FitnessCenter)
    object Feed     : Screen("feed",     Icons.Default.People)
    object Friends  : Screen("friends",  Icons.Default.PersonAdd)
    object History  : Screen("history",  Icons.Default.BarChart)
    object Calendar : Screen("calendar", Icons.Default.CalendarMonth)
    object Compare  : Screen("compare",  Icons.Default.Compare)
    object Settings : Screen("settings", Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home, Screen.Feed, Screen.Friends, Screen.History, Screen.Calendar, Screen.Compare
)

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    workoutViewModel: WorkoutViewModel,
    friendViewModel: FriendViewModel,
    onUserLoggedIn: () -> Unit = {},
    onAccentChanged: (String) -> Unit = {}
) {
    val isLoggedIn  by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val authState   by authViewModel.authState.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onUserLoggedIn()
    }

    when {
        !isLoggedIn -> {
            AuthScreen(authViewModel = authViewModel)
        }
        // Пока currentUser не загружен — ждём, ничего не показываем
        currentUser == null -> {
            Box(Modifier.fillMaxSize().background(AppBackground))
        }
        currentUser!!.displayName.isBlank() -> {
            SetDisplayNameScreen(
                authState = authState,
                onConfirm = { name -> authViewModel.setDisplayName(name) },
                onLogout  = { authViewModel.logout() }
            )
        }
        else -> {
            MainScaffold(
                authViewModel    = authViewModel,
                workoutViewModel = workoutViewModel,
                friendViewModel  = friendViewModel,
                onAccentChanged  = onAccentChanged
            )
        }
    }
}

@Composable
private fun SetDisplayNameScreen(
    authState: AuthState,
    onConfirm: (String) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val pillShape  = RoundedCornerShape(50.dp)
    val fieldShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("👤", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text       = "Как тебя зовут?",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = AppOnBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Имя будет видно друзьям",
                style = MaterialTheme.typography.bodyMedium,
                color = AppOnSurfaceVar
            )
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Имя") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = fieldShape,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = AppAccent,
                    unfocusedBorderColor    = AppSurfaceBright,
                    focusedLabelColor       = AppAccent,
                    unfocusedLabelColor     = AppOnSurfaceVar,
                    cursorColor             = AppAccent,
                    focusedTextColor        = AppOnBackground,
                    unfocusedTextColor      = AppOnBackground,
                    focusedContainerColor   = AppSurfaceVariant,
                    unfocusedContainerColor = AppSurfaceVariant,
                )
            )

            if (authState is AuthState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = (authState as? AuthState.Error)?.message ?: "",
                    color = AppError,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick   = { onConfirm(name) },
                modifier  = Modifier.fillMaxWidth().height(58.dp),
                enabled   = authState !is AuthState.Loading && name.isNotBlank(),
                shape     = pillShape,
                colors    = ButtonDefaults.buttonColors(
                    containerColor         = AppAccent,
                    contentColor           = AppBackground,
                    disabledContainerColor = AppAccentDim,
                    disabledContentColor   = AppOnSurfaceVar
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color       = AppBackground
                    )
                } else {
                    Text("Продолжить", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick  = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выйти из аккаунта", color = AppOnSurfaceVar)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScaffold(
    authViewModel: AuthViewModel,
    workoutViewModel: WorkoutViewModel,
    friendViewModel: FriendViewModel,
    onAccentChanged: (String) -> Unit = {}
) {
    val scope        = rememberCoroutineScope()
    val pagerState   = rememberPagerState(pageCount = { bottomNavItems.size })
    val pillShape    = RoundedCornerShape(50)
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showSettings) { showSettings = false }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .shadow(elevation = 20.dp, shape = pillShape, ambientColor = AppBackground)
                    .clip(pillShape)
                    .background(AppNavBar)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bottomNavItems.forEachIndexed { index, screen ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector    = screen.icon,
                            contentDescription = null,
                            tint           = if (selected) AppBackground else AppOnSurfaceVar
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        var chartInteracting by remember { mutableStateOf(false) }
        var inputActive      by remember { mutableStateOf(false) }
        val pagerScrollEnabled = !chartInteracting && !inputActive
        HorizontalPager(
            state             = pagerState,
            modifier          = Modifier.padding(innerPadding),
            userScrollEnabled = pagerScrollEnabled
        ) { page ->
            when (bottomNavItems[page]) {
                Screen.Home     -> HomeScreen(authViewModel, workoutViewModel, onInputActive = { inputActive = it })
                Screen.Feed     -> FeedScreen(authViewModel, workoutViewModel, friendViewModel)
                Screen.Friends  -> FriendsScreen(friendViewModel)
                Screen.History  -> HistoryScreen(workoutViewModel, onChartInteraction = { chartInteracting = it })
                Screen.Calendar -> CalendarScreen(workoutViewModel, friendViewModel)
                Screen.Compare  -> CompareScreen(authViewModel, workoutViewModel, friendViewModel)
                else            -> {}
            }
        }
    }

    // Кнопка настроек — правый верхний угол
    IconButton(
        onClick  = { showSettings = true },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(end = 4.dp)
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Настройки",
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Настройки как full-screen overlay
    AnimatedVisibility(
        visible = showSettings,
        enter   = slideInVertically { it },
        exit    = slideOutVertically { it }
    ) {
        SettingsScreen(
            onAccentChanged = onAccentChanged,
            onBack          = { showSettings = false },
            onLogout        = { authViewModel.logout(); showSettings = false }
        )
    }
    } // Box
}
