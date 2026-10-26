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
    fun yearModel_scalesBarsToBestIncomeOrExpenseOfTheYear() {
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
        assertEquals(100f / 200f, october.incomeFraction, 0.0001f)
        assertEquals(150f / 200f, october.expenseFraction, 0.0001f)
        assertEquals(1f, model.months[1].incomeFraction, 0.0001f)
        assertEquals(0f, model.months[1].expenseFraction, 0.0001f)
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
    fun monthModel_titleSubtitleAndWeekBars() {
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
        assertEquals(1f, model.weeks[1].incomeFraction, 0.0001f)
        assertEquals(20f / 60f, model.weeks[0].expenseFraction, 0.0001f)
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
}
