package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DailyFlow

enum class DateRangeOption(val label: String) {
    WEEK("Semana"),
    MONTH("Mes"),
    ALL("Todo")
}

data class MetricsState(
    val isLoading: Boolean = false,
    val selectedRange: DateRangeOption = DateRangeOption.MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalTransfers: Double = 0.0,
    val netFlow: Double = 0.0,
    val expensesByCategory: List<CategoryExpense> = emptyList(),
    val dailyFlows: List<DailyFlow> = emptyList()
)

sealed interface MetricsIntent {
    data class SelectRange(val range: DateRangeOption) : MetricsIntent
}

sealed interface MetricsEffect
