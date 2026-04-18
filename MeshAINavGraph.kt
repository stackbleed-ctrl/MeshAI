package com.meshai.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Tasks     : Screen("tasks")
    object Settings  : Screen("settings")
}

@Composable
fun MeshAINavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) { DashboardScreen() }
        composable(Screen.Tasks.route)     { TaskQueueScreen() }
        composable(Screen.Settings.route)  { SettingsScreen() }
    }
}
