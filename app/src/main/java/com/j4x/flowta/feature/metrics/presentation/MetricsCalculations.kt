package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.MetricsAggregator

internal data class DonutData(val slices: List<CategoryExpense>, val total: Double)

internal fun computeDonutData(categories: List<CategoryExpense>): DonutData {
    val slices = categories.filter { it.total > 0 }
    return DonutData(slices, slices.sumOf { it.total })
}

/** What the savings ring draws: [progress] 0..1, the text in the middle, and whether it is overspent. */
internal data class SavingsRingState(val progress: Float, val label: String, val isNegative: Boolean)

internal fun savingsRing(income: Double, expense: Double): SavingsRingState {
    val percent = MetricsAggregator.savingsPercent(income, expense)
        ?: return SavingsRingState(0f, "—", isNegative = false)
    return SavingsRingState((percent / 100f).coerceIn(0f, 1f), "$percent%", isNegative = percent < 0)
}
