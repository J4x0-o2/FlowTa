package com.j4x.flowta.feature.metrics.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class MetricsPeriodType { DAY, WEEK, MONTH, YEAR }

/**
 * A calendar period shown by Metrics. [start] is always normalized to the first day of the period:
 * the day itself, the Monday of a week, the 1st of a month or January 1st. Everything here works on
 * local dates; [range] converts to epoch millis for a given [ZoneId], so periods follow the user's
 * calendar (and daylight saving) rather than UTC.
 */
data class MetricsPeriod(val type: MetricsPeriodType, val start: LocalDate) {

    /** First day after the period. */
    val endExclusive: LocalDate
        get() = when (type) {
            MetricsPeriodType.DAY -> start.plusDays(1)
            MetricsPeriodType.WEEK -> start.plusWeeks(1)
            MetricsPeriodType.MONTH -> start.plusMonths(1)
            MetricsPeriodType.YEAR -> start.plusYears(1)
        }

    val lengthDays: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()

    /** The period [n] steps away (negative goes back) of the same type. */
    fun shift(n: Int): MetricsPeriod {
        val amount = n.toLong()
        val moved = when (type) {
            MetricsPeriodType.DAY -> start.plusDays(amount)
            MetricsPeriodType.WEEK -> start.plusWeeks(amount)
            MetricsPeriodType.MONTH -> start.plusMonths(amount)
            MetricsPeriodType.YEAR -> start.plusYears(amount)
        }
        return copy(start = moved)
    }

    fun previous(): MetricsPeriod = shift(-1)

    fun next(): MetricsPeriod = shift(1)

    operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive

    fun isCurrent(today: LocalDate): Boolean = today in this

    /** Days of the period up to and including [today], from 0 (not started) to [lengthDays]. */
    fun elapsedDays(today: LocalDate): Int = when {
        today < start -> 0
        today >= endExclusive -> lengthDays
        else -> ChronoUnit.DAYS.between(start, today).toInt() + 1
    }

    /** `[from, to)` as epoch millis: local midnight to local midnight in [zone]. */
    fun range(zone: ZoneId): DateRange = DateRange(
        from = start.atStartOfDay(zone).toInstant().toEpochMilli(),
        to = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    )

    companion object {
        /** The period of [type] that contains [date]. Weeks run Monday to Sunday. */
        fun containing(type: MetricsPeriodType, date: LocalDate): MetricsPeriod = MetricsPeriod(
            type,
            when (type) {
                MetricsPeriodType.DAY -> date
                MetricsPeriodType.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                MetricsPeriodType.MONTH -> date.withDayOfMonth(1)
                MetricsPeriodType.YEAR -> date.withDayOfYear(1)
            }
        )
    }
}
