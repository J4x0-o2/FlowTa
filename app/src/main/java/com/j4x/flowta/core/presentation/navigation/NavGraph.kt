package com.j4x.flowta.core.presentation.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

    // Duración común corta + easing de desaceleración: transiciones ágiles y consistentes.
    val durationMs = 200
    val easing     = FastOutSlowInEasing

    NavHost(
        navController  = navController,
        startDestination = Screen.Transactions.route,
        // Entrada al navegar hacia adelante: la pantalla nueva entra deslizando desde la derecha.
        enterTransition = {
            slideInHorizontally(animationSpec = tween(durationMs, easing = easing)) { fullWidth -> fullWidth / 4 } +
                fadeIn(animationSpec = tween(durationMs, easing = easing))
        },
        // La pantalla anterior se va deslizando un poco hacia la izquierda y se desvanece.
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(durationMs, easing = easing)) { fullWidth -> -fullWidth / 4 } +
                fadeOut(animationSpec = tween(durationMs, easing = easing))
        },
        // Al volver (back): la pantalla anterior reaparece desde la izquierda.
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(durationMs, easing = easing)) { fullWidth -> -fullWidth / 4 } +
                fadeIn(animationSpec = tween(durationMs, easing = easing))
        },
        // La pantalla actual sale deslizando hacia la derecha al hacer back.
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(durationMs, easing = easing)) { fullWidth -> fullWidth / 4 } +
                fadeOut(animationSpec = tween(durationMs, easing = easing))
        }
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
