package com.j4x.flowta.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.j4x.flowta.feature.accounts.presentation.AccountDetailScreen
import com.j4x.flowta.feature.goals.presentation.GoalDetailScreen
import com.j4x.flowta.feature.goals.presentation.GoalsScreen
import com.j4x.flowta.feature.metrics.presentation.MetricsScreen
import com.j4x.flowta.feature.profile.presentation.ProfileScreen
import com.j4x.flowta.feature.transactions.presentation.TransactionScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController  = navController,
        startDestination = Screen.Transactions.route
    ) {
        composable(Screen.Transactions.route) {
            TransactionScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.Goals.route) {
            GoalsScreen(navController = navController)
        }
        composable(Screen.Metrics.route) {
            MetricsScreen(navController = navController)
        }
        composable(
            route = Screen.AccountDetail().route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) {
            AccountDetailScreen(navController = navController)
        }
        composable(
            route = Screen.GoalDetail().route,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getString("goalId") ?: return@composable
            GoalDetailScreen(goalId = goalId, navController = navController)
        }
    }
}
