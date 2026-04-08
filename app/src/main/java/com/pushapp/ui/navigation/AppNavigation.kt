package com.pushapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pushapp.ui.screens.*
import com.pushapp.ui.theme.AppBackground
import com.pushapp.ui.theme.AppNavBar
import com.pushapp.ui.theme.AppOnSurfaceVar
import com.pushapp.viewmodel.AuthViewModel
import com.pushapp.viewmodel.WorkoutViewModel

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
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val floatingShape = RoundedCornerShape(32.dp)

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(elevation = 24.dp, shape = floatingShape, ambientColor = AppBackground)
                        .clip(floatingShape),
                    containerColor = AppNavBar,
                    tonalElevation = 0.dp,
                    windowInsets   = WindowInsets(0)
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
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
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route)     { HomeScreen(authViewModel, workoutViewModel) }
            composable(Screen.Feed.route)     { FeedScreen(authViewModel, workoutViewModel) }
            composable(Screen.History.route)  { HistoryScreen(workoutViewModel) }
            composable(Screen.Calendar.route) { CalendarScreen(authViewModel, workoutViewModel) }
            composable(Screen.Compare.route)  { CompareScreen(authViewModel, workoutViewModel) }
            composable(Screen.Settings.route) { SettingsScreen(onAccentChanged = onAccentChanged) }
        }
    }
}
