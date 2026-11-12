package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsCalculationsTest {

    private fun cat(name: String, total: Double) = CategoryExpense(name, name, "EXPENSE", total)

    @Test
    fun donut_dropsNonPositiveCategoriesAndSumsTotal() {
        val data = computeDonutData(listOf(cat("a", 30.0), cat("b", 0.0), cat("c", 70.0), cat("d", -5.0)))
        assertEquals(listOf("a", "c"), data.slices.map { it.typeName })
        assertEquals(100.0, data.total, 0.0)
    }

    @Test
    fun donut_emptyOrAllZeroHasZeroTotal() {
        assertEquals(0.0, computeDonutData(emptyList()).total, 0.0)
        assertEquals(0.0, computeDonutData(listOf(cat("a", 0.0))).total, 0.0)
    }
}
