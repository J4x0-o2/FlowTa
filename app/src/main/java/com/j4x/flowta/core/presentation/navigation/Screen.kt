package com.j4x.flowta.core.presentation.navigation

sealed class Screen(val route: String) {
    data object Transactions : Screen("transactions")
    data object Profile      : Screen("profile")
    data object Goals        : Screen("goals")
    data object Metrics      : Screen("metrics")

    data class AccountDetail(val accountId: String = "{accountId}") :
        Screen("account_detail/{accountId}") {
        fun createRoute(id: String) = "account_detail/$id"
    }

    data class GoalDetail(val goalId: String = "{goalId}") :
        Screen("goal_detail/{goalId}") {
        fun createRoute(id: String) = "goal_detail/$id"
    }
}
