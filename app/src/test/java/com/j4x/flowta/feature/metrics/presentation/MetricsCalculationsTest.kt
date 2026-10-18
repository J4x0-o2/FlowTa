package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DailyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsCalculationsTest {

    private fun flow(type: String, total: Double, day: Long) = DailyFlow(type, total, day)
    private fun cat(name: String, total: Double) = CategoryExpense(name, name, "EXPENSE", total)

    @Test
    fun barChart_groupsIncomeAndExpensePerDaySortedAscending() {
        val data = computeBarChartData(
            listOf(
                flow("EXPENSE", 30.0, 2L),
                flow("INCOME", 100.0, 2L),
                flow("EXPENSE", 10.0, 1L)
            )
        )
        assertEquals(
            listOf(DayBars(1L, 0f, 10f), DayBars(2L, 100f, 30f)),
            data.days
        )
        assertEquals(100f, data.maxValue, 0f)
    }

    @Test
    fun barChart_keepsOnlyLast14Days() {
        val flows = (1L..20L).map { flow("INCOME", it.toDouble(), it) }
        val data = computeBarChartData(flows)
        assertEquals(14, data.days.size)
        assertEquals(7L, data.days.first().dayBucket)
        assertEquals(20L, data.days.last().dayBucket)
        assertEquals(20f, data.maxValue, 0f)
    }

    @Test
    fun barChart_maxValueIsAtLeastOneAndEmptyInputGivesNoDays() {
        assertEquals(1f, computeBarChartData(listOf(flow("INCOME", 0.25, 1L))).maxValue, 0f)
        val empty = computeBarChartData(emptyList())
        assertEquals(emptyList<DayBars>(), empty.days)
        assertEquals(1f, empty.maxValue, 0f)
    }

    @Test
    fun barChart_maxValueConsidersDaysOutsideTheLast14() {
        // Legacy behavior: max is taken over all flows, not only the visible days.
        val flows = listOf(flow("INCOME", 500.0, 1L)) + (2L..16L).map { flow("EXPENSE", 5.0, it) }
        assertEquals(500f, computeBarChartData(flows).maxValue, 0f)
    }

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
