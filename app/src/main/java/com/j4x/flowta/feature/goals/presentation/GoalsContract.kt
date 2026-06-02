package com.j4x.flowta.feature.goals.presentation

import com.j4x.flowta.feature.goals.domain.model.Goal

data class GoalsState(
    val isLoading: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val error: String? = null
)

sealed interface GoalsIntent {
    data class NavigateToDetail(val goalId: String) : GoalsIntent
    data class AddGoal(
        val name: String, val targetAmount: Double,
        val mode: String, val periodicity: String, val deadline: Long?
    ) : GoalsIntent
    data class DeleteGoal(val id: String) : GoalsIntent
}

sealed interface GoalsEffect {
    data class NavigateToDetail(val goalId: String) : GoalsEffect
    data class ShowError(val message: String) : GoalsEffect
}
