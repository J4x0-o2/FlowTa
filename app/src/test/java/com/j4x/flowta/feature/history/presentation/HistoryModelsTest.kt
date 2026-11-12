package com.j4x.flowta.feature.history.presentation

import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.feature.history.domain.model.MonthDetail
import com.j4x.flowta.feature.history.domain.model.MonthHistory
import com.j4x.flowta.feature.history.domain.model.PeriodTotals
import com.j4x.flowta.feature.history.domain.model.WeekHistory
import com.j4x.flowta.feature.history.domain.model.YearHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HistoryModelsTest {

    @Test
    fun monthName_isCapitalizedSpanish() {
        assertEquals("Octubre", monthName(YearMonth.of(2026, 10)))
        assertEquals("Enero", monthName(YearMonth.of(2026, 1)))
    }

    @Test
    fun weekRange_sameMonth_clippedAtMonthStart() {
        assertEquals("1 – 4 oct", weekRangeText(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4)))
    }

    @Test
    fun weekRange_singleDay() {
        assertEquals("31 oct", weekRangeText(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun weekRange_acrossMonths_showsBothMonths() {
        assertEquals("29 sep – 5 oct", weekRangeText(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun yearModel_eachMonthStandsAlone() {
        val history = YearHistory(
            year = 2026,
            totals = PeriodTotals(300.0, 150.0, 5),
            months = listOf(
                MonthHistory(YearMonth.of(2026, 10), PeriodTotals(100.0, 150.0, 2), isCurrent = true),
                MonthHistory(YearMonth.of(2026, 3), PeriodTotals(200.0, 0.0, 3), isCurrent = false)
            ),
            hasPreviousYear = false,
            isCurrentYear = true
        )

        val model = YearHistoryModel.from(history)

        assertEquals(150.0, model.net, 0.0001)
        assertFalse(model.canGoBack)
        assertFalse(model.canGoForward)
        assertFalse(model.isEmpty)
        val october = model.months[0]
        assertEquals("Octubre", october.name)
        assertTrue(october.isCurrent)
        assertEquals(-50.0, october.net, 0.0001)
        assertEquals(SpendShare.Over(50.0), october.spendShare)
        assertEquals(SpendShare.Within(0f, 0), model.months[1].spendShare)
    }

    @Test
    fun yearModel_emptyYear_isEmptyWithZeroBars() {
        val model = YearHistoryModel.from(YearHistory(2025, PeriodTotals.ZERO, emptyList(), hasPreviousYear = true, isCurrentYear = false))
        assertTrue(model.isEmpty)
        assertTrue(model.canGoBack)
        assertTrue(model.canGoForward)
        assertEquals(0.0, model.net, 0.0001)
    }

    @Test
    fun monthModel_titleSubtitleAndSpendShares() {
        val detail = MonthDetail(
            month = YearMonth.of(2026, 10),
            totals = PeriodTotals(60.0, 20.0, 3),
            weeks = listOf(
                WeekHistory(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4), PeriodTotals(60.0, 0.0, 2), isCurrent = false),
                WeekHistory(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11), PeriodTotals(0.0, 20.0, 1), isCurrent = true)
            )
        )

        val model = MonthHistoryModel.from(detail)

        assertEquals("Octubre", model.title)
        assertEquals(2026, model.year)
        assertEquals(3, model.count)
        assertEquals(40.0, model.net, 0.0001)
        // Newest week first.
        assertEquals("1 – 4 oct", model.weeks[1].rangeText)
        assertEquals(SpendShare.Within(0f, 0), model.weeks[1].spendShare)
        assertEquals(SpendShare.Over(20.0), model.weeks[0].spendShare)
        assertEquals(SpendShare.Within(20f / 60f, 33), model.spendShare)
        assertTrue(model.weeks[0].isCurrent)
        assertEquals(-20.0, model.weeks[0].net, 0.0001)
    }

    @Test
    fun monthModel_weeksAreNewestFirst() {
        val detail = MonthDetail(
            YearMonth.of(2026, 10), PeriodTotals.ZERO,
            listOf(
                WeekHistory(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4), PeriodTotals.ZERO, false),
                WeekHistory(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11), PeriodTotals.ZERO, true)
            )
        )
        assertEquals(listOf("5 – 11 oct", "1 – 4 oct"), MonthHistoryModel.from(detail).weeks.map { it.rangeText })
    }

    @Test
    fun formatMoney_cop_groupsThousandsAndPrefixesMinus() {
        assertEquals("$1,250", formatMoney(1250.0, "$", CurrencyCode.COP))
        assertEquals("-$1,250", formatMoney(-1250.0, "$", CurrencyCode.COP))
        assertEquals("$0", formatMoney(0.0, "$", CurrencyCode.COP))
    }

    @Test
    fun formatIncomeAndExpense_carrySign_exceptZero() {
        assertEquals("+$500", formatIncome(500.0, "$", CurrencyCode.COP))
        assertEquals("−$500", formatExpense(500.0, "$", CurrencyCode.COP))
        assertEquals("$0", formatIncome(0.0, "$", CurrencyCode.COP))
        assertEquals("$0", formatExpense(0.0, "$", CurrencyCode.COP))
    }

    @Test
    fun spendShare_normal_isFractionAndRoundedPercent() {
        assertWithin(0.65f, 65, spendShare(income = 1000.0, expense = 650.0))
        assertWithin(1f / 3f, 33, spendShare(income = 300.0, expense = 100.0))
        assertWithin(2f / 3f, 67, spendShare(income = 300.0, expense = 200.0))
    }

    private fun assertWithin(fraction: Float, percent: Int, actual: SpendShare) {
        val within = actual as SpendShare.Within
        assertEquals(fraction, within.fraction, 0.0001f)
        assertEquals(percent, within.percent)
    }

    @Test
    fun spendShare_exactlyAllIncomeSpent_isWithinAt100() {
        assertEquals(SpendShare.Within(1f, 100), spendShare(income = 500.0, expense = 500.0))
    }

    @Test
    fun spendShare_noExpense_isEmptyBarAtZeroPercent() {
        assertEquals(SpendShare.Within(0f, 0), spendShare(income = 500.0, expense = 0.0))
    }

    @Test
    fun spendShare_expenseAboveIncome_isOverWithExcess() {
        assertEquals(SpendShare.Over(250.0), spendShare(income = 500.0, expense = 750.0))
    }

    @Test
    fun spendShare_zeroIncomeWithExpense_isOverWithWholeExpense() {
        assertEquals(SpendShare.Over(80.0), spendShare(income = 0.0, expense = 80.0))
    }

    @Test
    fun spendShare_bothZero_isNone() {
        assertEquals(SpendShare.None, spendShare(income = 0.0, expense = 0.0))
    }
}
