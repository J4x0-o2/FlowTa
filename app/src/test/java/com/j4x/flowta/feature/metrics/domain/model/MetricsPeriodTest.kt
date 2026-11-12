package com.j4x.flowta.feature.metrics.domain.model

import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.DAY
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.MONTH
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.WEEK
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.YEAR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MetricsPeriodTest {

    private fun date(text: String) = LocalDate.parse(text)

    // --- containing ---

    @Test
    fun day_startsAndEndsOnTheSameCalendarDay() {
        val period = MetricsPeriod.containing(DAY, date("2026-11-11"))
        assertEquals(date("2026-11-11"), period.start)
        assertEquals(date("2026-11-12"), period.endExclusive)
        assertEquals(1, period.lengthDays)
    }

    @Test
    fun week_runsMondayToSunday() {
        // 2026-11-11 is a Wednesday.
        val period = MetricsPeriod.containing(WEEK, date("2026-11-11"))
        assertEquals(date("2026-11-09"), period.start)
        assertEquals(date("2026-11-16"), period.endExclusive)
        assertEquals(7, period.lengthDays)
    }

    @Test
    fun week_mondayStartsItAndSundayClosesIt() {
        assertEquals(date("2026-11-09"), MetricsPeriod.containing(WEEK, date("2026-11-09")).start)
        assertEquals(date("2026-11-09"), MetricsPeriod.containing(WEEK, date("2026-11-15")).start)
        assertEquals(date("2026-11-16"), MetricsPeriod.containing(WEEK, date("2026-11-16")).start)
        assertEquals(date("2026-11-02"), MetricsPeriod.containing(WEEK, date("2026-11-08")).start)
    }

    @Test
    fun week_canStraddleMonthAndYearBoundaries() {
        val period = MetricsPeriod.containing(WEEK, date("2026-12-31"))
        assertEquals(date("2026-12-28"), period.start)
        assertEquals(date("2027-01-04"), period.endExclusive)
    }

    @Test
    fun month_hasTheLengthOfThatCalendarMonth() {
        assertEquals(30, MetricsPeriod.containing(MONTH, date("2026-11-11")).lengthDays)
        assertEquals(31, MetricsPeriod.containing(MONTH, date("2026-12-01")).lengthDays)
        assertEquals(28, MetricsPeriod.containing(MONTH, date("2026-02-14")).lengthDays)
        assertEquals(29, MetricsPeriod.containing(MONTH, date("2028-02-29")).lengthDays)
        assertEquals(date("2026-11-01"), MetricsPeriod.containing(MONTH, date("2026-11-30")).start)
    }

    @Test
    fun year_hasTheLengthOfThatCalendarYear() {
        val leap = MetricsPeriod.containing(YEAR, date("2028-06-15"))
        assertEquals(date("2028-01-01"), leap.start)
        assertEquals(date("2029-01-01"), leap.endExclusive)
        assertEquals(366, leap.lengthDays)
        assertEquals(365, MetricsPeriod.containing(YEAR, date("2026-12-31")).lengthDays)
    }

    // --- navigation ---

    @Test
    fun previousAndNext_stepOnePeriodOfTheSameType() {
        assertEquals(date("2026-11-10"), MetricsPeriod.containing(DAY, date("2026-11-11")).previous().start)
        assertEquals(date("2026-11-02"), MetricsPeriod.containing(WEEK, date("2026-11-11")).previous().start)
        assertEquals(date("2026-12-01"), MetricsPeriod.containing(MONTH, date("2026-11-11")).next().start)
        assertEquals(date("2025-01-01"), MetricsPeriod.containing(YEAR, date("2026-11-11")).previous().start)
    }

    @Test
    fun month_stepsAcrossYearBoundaries() {
        assertEquals(date("2025-12-01"), MetricsPeriod.containing(MONTH, date("2026-01-20")).previous().start)
        assertEquals(date("2027-01-01"), MetricsPeriod.containing(MONTH, date("2026-12-20")).next().start)
    }

    @Test
    fun shift_movesSeveralPeriodsAndIsReversible() {
        val week = MetricsPeriod.containing(WEEK, date("2026-11-11"))
        assertEquals(date("2026-10-12"), week.shift(-4).start)
        assertEquals(week, week.shift(-4).shift(4))
        assertEquals(week, week.shift(0))
    }

    @Test
    fun monthShift_neverDriftsToAShorterMonthDay() {
        val january = MetricsPeriod.containing(MONTH, date("2026-01-31"))
        assertEquals(date("2026-02-01"), january.next().start)
        assertEquals(date("2026-03-01"), january.next().next().start)
    }

    // --- today ---

    @Test
    fun containsAndIsCurrent_followLocalDates() {
        val week = MetricsPeriod.containing(WEEK, date("2026-11-11"))
        assertTrue(week.contains(date("2026-11-09")))
        assertTrue(week.contains(date("2026-11-15")))
        assertFalse(week.contains(date("2026-11-16")))
        assertFalse(week.contains(date("2026-11-08")))
        assertTrue(week.isCurrent(date("2026-11-13")))
        assertFalse(week.previous().isCurrent(date("2026-11-13")))
    }

    @Test
    fun elapsedDays_countsFromStartThroughToday_capped() {
        val week = MetricsPeriod.containing(WEEK, date("2026-11-11"))
        assertEquals(1, week.elapsedDays(date("2026-11-09")))
        assertEquals(3, week.elapsedDays(date("2026-11-11")))
        assertEquals(7, week.elapsedDays(date("2026-11-15")))
        assertEquals(7, week.elapsedDays(date("2026-11-20")))
        assertEquals(0, week.next().elapsedDays(date("2026-11-11")))
        assertEquals(11, MetricsPeriod.containing(MONTH, date("2026-11-01")).elapsedDays(date("2026-11-11")))
    }

    // --- range ---

    @Test
    fun range_isLocalMidnightToLocalMidnightInEpochMillis() {
        val bogota = ZoneId.of("America/Bogota") // UTC-5, no DST
        val range = MetricsPeriod.containing(DAY, date("2026-11-11")).range(bogota)
        assertEquals(Instant.parse("2026-11-11T05:00:00Z").toEpochMilli(), range.from)
        assertEquals(Instant.parse("2026-11-12T05:00:00Z").toEpochMilli(), range.to)
    }

    @Test
    fun range_followsDaylightSavingInsteadOfAssuming24Hours() {
        val newYork = ZoneId.of("America/New_York") // DST starts Sun 2026-03-08
        val week = MetricsPeriod.containing(WEEK, date("2026-03-04"))
        val range = week.range(newYork)
        assertEquals(167L * 3_600_000L, range.to - range.from)
        // Adjacent periods share the boundary exactly: no gap, no overlap.
        assertEquals(range.from, week.next().previous().range(newYork).from)
        assertEquals(range.to, week.next().range(newYork).from)
    }
}
