package com.j4x.flowta.feature.history.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class HistoryAggregatorTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val today = LocalDate.of(2026, 10, 25) // Sunday

    private fun at(date: String, time: String = "12:00", zone: ZoneId = bogota): Long =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private fun income(date: String, amount: Double, time: String = "12:00") =
        HistoryMovement("INCOME", amount, at(date, time))

    private fun expense(date: String, amount: Double, time: String = "12:00") =
        HistoryMovement("EXPENSE", amount, at(date, time))

    private fun year(year: Int, movements: List<HistoryMovement>, earliest: Long? = null, now: LocalDate = today) =
        HistoryAggregator.year(year, movements, earliest, now, bogota)

    private fun month(month: YearMonth, movements: List<HistoryMovement>, now: LocalDate = today) =
        HistoryAggregator.month(month, movements, now, bogota)

    @Test
    fun totals_net_isIncomeMinusExpense() {
        val totals = PeriodTotals(income = 100.0, expense = 130.5, count = 2)
        assertEquals(-30.5, totals.net, 0.0001)
    }

    @Test
    fun year_sumsIncomeExpenseAndCount() {
        val result = year(2026, listOf(income("2026-03-01", 100.0), expense("2026-03-02", 40.0), income("2026-10-02", 10.0)))
        assertEquals(PeriodTotals(110.0, 40.0, 3), result.totals)
        assertEquals(70.0, result.totals.net, 0.0001)
    }

    @Test
    fun year_omitsMonthsWithoutMovements_newestFirst() {
        val result = year(2026, listOf(income("2026-01-15", 5.0), income("2026-10-02", 5.0), expense("2026-04-02", 1.0)))
        assertEquals(
            listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 4), YearMonth.of(2026, 1)),
            result.months.map { it.month }
        )
    }

    @Test
    fun year_marksCurrentMonthOnly() {
        val result = year(2026, listOf(income("2026-09-30", 5.0), income("2026-10-02", 5.0)))
        assertEquals(listOf(true, false), result.months.map { it.isCurrent })
    }

    @Test
    fun year_ignoresMovementsOutsideTheYear() {
        val result = year(2026, listOf(income("2025-12-31", 99.0), income("2026-01-01", 1.0)))
        assertEquals(PeriodTotals(1.0, 0.0, 1), result.totals)
    }

    @Test
    fun year_localZoneBoundary_lateOctoberNightBelongsToOctober() {
        // 2026-10-31T23:30 in Bogota is 2026-11-01T04:30Z: November in UTC, October locally.
        val result = year(2026, listOf(income("2026-10-31", 50.0, time = "23:30")))
        assertEquals(listOf(YearMonth.of(2026, 10)), result.months.map { it.month })
    }

    @Test
    fun year_localZoneBoundary_yearEndNightBelongsToThatYear() {
        val movement = income("2026-12-31", 7.0, time = "23:59")
        assertEquals(1, year(2026, listOf(movement)).totals.count)
        assertEquals(0, year(2027, listOf(movement)).totals.count)
    }

    @Test
    fun year_navigation_flags() {
        val earliest = at("2025-06-01")
        val current = year(2026, emptyList(), earliest)
        assertTrue(current.hasPreviousYear)
        assertTrue(current.isCurrentYear)
        val firstYear = year(2025, emptyList(), earliest)
        assertFalse(firstYear.hasPreviousYear)
        assertFalse(firstYear.isCurrentYear)
        assertFalse(year(2026, emptyList(), earliest = null).hasPreviousYear)
    }

    @Test
    fun year_earliestJudgedInLocalZone() {
        // 2026-01-01T00:30 local is still 2025-12-31 in UTC; the previous year must not appear.
        val earliest = at("2026-01-01", "00:30")
        assertFalse(year(2026, emptyList(), earliest).hasPreviousYear)
    }

    @Test
    fun year_emptyYear_hasNoMonthsAndZeroTotals() {
        val result = year(2026, emptyList())
        assertTrue(result.months.isEmpty())
        assertEquals(PeriodTotals(0.0, 0.0, 0), result.totals)
    }

    @Test
    fun month_pastMonth_listsEveryWeekClippedToTheMonth() {
        // October 2026 starts on Thursday and ends on Saturday.
        val result = month(YearMonth.of(2026, 10), emptyList(), now = LocalDate.of(2026, 11, 20))
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 10, 5) to LocalDate.of(2026, 10, 11),
                LocalDate.of(2026, 10, 12) to LocalDate.of(2026, 10, 18),
                LocalDate.of(2026, 10, 19) to LocalDate.of(2026, 10, 25),
                LocalDate.of(2026, 10, 26) to LocalDate.of(2026, 10, 31)
            ),
            result.weeks.map { it.startDate to it.endDate }
        )
    }

    @Test
    fun month_pastMonth_includesZeroWeeks() {
        val result = month(YearMonth.of(2026, 10), listOf(income("2026-10-27", 9.0)), now = LocalDate.of(2026, 11, 20))
        assertEquals(5, result.weeks.size)
        assertEquals(PeriodTotals.ZERO, result.weeks[0].totals)
        assertEquals(PeriodTotals(9.0, 0.0, 1), result.weeks[4].totals)
    }

    @Test
    fun month_currentMonth_stopsAtTheWeekContainingToday() {
        val result = month(YearMonth.of(2026, 10), emptyList(), now = LocalDate.of(2026, 10, 14)) // Wednesday
        assertEquals(3, result.weeks.size)
        assertEquals(LocalDate.of(2026, 10, 18), result.weeks.last().endDate) // whole week, not cut at today
        assertTrue(result.weeks.last().isCurrent)
        assertFalse(result.weeks.first().isCurrent)
    }

    @Test
    fun month_weekSpanningTwoMonths_isClippedAtMonthEnd() {
        // September 2026 ends on Wednesday the 30th.
        val result = month(YearMonth.of(2026, 9), emptyList(), now = LocalDate.of(2026, 10, 25))
        val last = result.weeks.last()
        assertEquals(LocalDate.of(2026, 9, 28) to LocalDate.of(2026, 9, 30), last.startDate to last.endDate)
    }

    @Test
    fun month_groupsMovementsByLocalDayIntoWeeks() {
        val result = month(
            YearMonth.of(2026, 10),
            listOf(
                income("2026-10-04", 10.0, time = "23:30"), // Sunday night local, Monday in UTC
                expense("2026-10-05", 3.0, time = "00:10"),
                income("2026-10-31", 1.0, time = "23:30") // Saturday night local, Nov 1 in UTC
            ),
            now = LocalDate.of(2026, 11, 20)
        )
        assertEquals(PeriodTotals(10.0, 0.0, 1), result.weeks[0].totals)
        assertEquals(PeriodTotals(0.0, 3.0, 1), result.weeks[1].totals)
        assertEquals(PeriodTotals(1.0, 0.0, 1), result.weeks[4].totals)
        assertEquals(PeriodTotals(11.0, 3.0, 3), result.totals)
    }

    @Test
    fun month_ignoresMovementsOfOtherMonths() {
        val result = month(
            YearMonth.of(2026, 10),
            listOf(income("2026-09-30", 4.0), income("2026-11-01", 4.0)),
            now = LocalDate.of(2026, 11, 20)
        )
        assertEquals(PeriodTotals.ZERO, result.totals)
    }

    @Test
    fun barFraction_scalesToMaxAndClamps() {
        assertEquals(0.5f, HistoryAggregator.barFraction(50.0, 100.0), 0.0001f)
        assertEquals(1f, HistoryAggregator.barFraction(100.0, 100.0), 0.0001f)
        assertEquals(1f, HistoryAggregator.barFraction(150.0, 100.0), 0.0001f)
        assertEquals(0f, HistoryAggregator.barFraction(10.0, 0.0), 0.0001f)
        assertEquals(0f, HistoryAggregator.barFraction(-5.0, 100.0), 0.0001f)
    }
}
