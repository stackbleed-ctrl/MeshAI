package com.meshai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meshai.ui.screens.DashboardScreen
import com.meshai.ui.screens.MeshMapScreen
import com.meshai.ui.screens.TaskQueueScreen
import com.meshai.ui.screens.GoalInputScreen
import com.meshai.ui.screens.SettingsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object MeshMap : Screen("mesh_map")
    object TaskQueue : Screen("task_queue")
    object GoalInput : Screen("goal_input")
    object Settings : Screen("settings")
}

@Composable
fun MeshAINavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        composable(Screen.MeshMap.route) {
            MeshMapScreen(navController = navController)
        }
        composable(Screen.TaskQueue.route) {
            TaskQueueScreen(navController = navController)
        }
        composable(Screen.GoalInput.route) {
            GoalInputScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
