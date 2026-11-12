package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense

internal data class DonutData(val slices: List<CategoryExpense>, val total: Double)

internal fun computeDonutData(categories: List<CategoryExpense>): DonutData {
    val slices = categories.filter { it.total > 0 }
    return DonutData(slices, slices.sumOf { it.total })
}
