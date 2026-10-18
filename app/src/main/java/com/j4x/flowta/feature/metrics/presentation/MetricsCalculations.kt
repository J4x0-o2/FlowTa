package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DailyFlow

internal const val MAX_BAR_DAYS = 14

/** Income and expense totals of one day bucket. */
internal data class DayBars(val dayBucket: Long, val income: Float, val expense: Float)

internal data class BarChartData(val days: List<DayBars>, val maxValue: Float)

internal data class DonutData(val slices: List<CategoryExpense>, val total: Double)

internal fun computeBarChartData(dailyFlows: List<DailyFlow>): BarChartData {
    val grouped  = dailyFlows.groupBy { it.dayBucket }
    val days     = grouped.keys.sorted().takeLast(MAX_BAR_DAYS).map { bucket ->
        val data = grouped.getValue(bucket)
        DayBars(
            dayBucket = bucket,
            income    = data.find { it.flowType == "INCOME"  }?.total?.toFloat() ?: 0f,
            expense   = data.find { it.flowType == "EXPENSE" }?.total?.toFloat() ?: 0f
        )
    }
    val maxValue = (dailyFlows.maxOfOrNull { it.total } ?: 0.0).coerceAtLeast(1.0).toFloat()
    return BarChartData(days, maxValue)
}

internal fun computeDonutData(categories: List<CategoryExpense>): DonutData {
    val slices = categories.filter { it.total > 0 }
    return DonutData(slices, slices.sumOf { it.total })
}
