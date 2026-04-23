package com.pushapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pushapp.ui.screens.*
import com.pushapp.ui.theme.AppBackground
import com.pushapp.ui.theme.AppNavBar
import com.pushapp.ui.theme.AppOnSurfaceVar
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home     : Screen("home",     Icons.Default.FitnessCenter)
    object Feed     : Screen("feed",     Icons.Default.People)
    object History  : Screen("history",  Icons.Default.BarChart)
    object Calendar : Screen("calendar", Icons.Default.CalendarMonth)
    object Compare  : Screen("compare",  Icons.Default.Compare)
    object Settings : Screen("settings", Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home, Screen.Feed, Screen.History, Screen.Calendar, Screen.Compare, Screen.Settings
)

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    workoutViewModel: WorkoutViewModel,
    onUserLoggedIn: () -> Unit = {},
    onAccentChanged: (String) -> Unit = {}
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) onUserLoggedIn()
    }

    if (!isLoggedIn) {
        AuthScreen(authViewModel = authViewModel)
    } else {
        MainScaffold(
            authViewModel    = authViewModel,
            workoutViewModel = workoutViewModel,
            onAccentChanged  = onAccentChanged
        )
    }
}

@Composable
private fun MainScaffold(
    authViewModel: AuthViewModel,
    workoutViewModel: WorkoutViewModel,
    onAccentChanged: (String) -> Unit = {}
) {
    val scope      = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { bottomNavItems.size })
    val pillShape  = RoundedCornerShape(50)

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .shadow(elevation = 20.dp, shape = pillShape, ambientColor = AppBackground)
                        .clip(pillShape)
                        .background(AppNavBar)
                )
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets   = WindowInsets(0)
                ) {
                    bottomNavItems.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon     = { Icon(screen.icon, contentDescription = null) },
                            selected = pagerState.currentPage == index,
                            onClick  = { scope.launch { pagerState.animateScrollToPage(index) } },
                            colors   = NavigationBarItemDefaults.colors(
                                selectedIconColor   = AppBackground,
                                unselectedIconColor = AppOnSurfaceVar,
                                indicatorColor      = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.padding(innerPadding),
            beyondViewportPageCount = 1
        ) { page ->
            when (bottomNavItems[page]) {
                Screen.Home     -> HomeScreen(authViewModel, workoutViewModel)
                Screen.Feed     -> FeedScreen(authViewModel, workoutViewModel)
                Screen.History  -> HistoryScreen(workoutViewModel)
                Screen.Calendar -> CalendarScreen(authViewModel, workoutViewModel)
                Screen.Compare  -> CompareScreen(authViewModel, workoutViewModel)
                Screen.Settings -> SettingsScreen(onAccentChanged = onAccentChanged)
            }
        }
    }
}
