package com.j4x.flowta.feature.goals.domain.model

data class Goal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val mode: GoalMode,
    val periodicity: GoalPeriodicity,
    val deadline: Long?,
    val createdAt: Long
) {
    val progress: Float get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat() else 0f
}

data class GoalContribution(
    val id: String,
    val amount: Double,
    val note: String?,
    val createdAt: Long,
    val fromAccountName: String
)

enum class GoalMode { SAVINGS, SAVINGS_FOR_EXPENSE }

enum class GoalPeriodicity { DAILY, WEEKLY, BIWEEKLY, MONTHLY, BIMONTHLY, QUARTERLY, SEMIANNUAL }
