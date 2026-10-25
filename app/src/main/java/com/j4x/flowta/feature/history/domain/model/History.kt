package com.j4x.flowta.feature.history.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** One income or expense reduced to what the history needs. Transfers are never part of it. */
data class HistoryMovement(val flowType: String, val amount: Double, val createdAt: Long)

/** Income and expense of a slice of history. */
data class PeriodTotals(val income: Double, val expense: Double, val count: Int) {
    /** Income minus expense; negative when more was spent than earned. */
    val net: Double get() = income - expense

    companion object {
        val ZERO = PeriodTotals(0.0, 0.0, 0)
    }
}

data class MonthHistory(val month: YearMonth, val totals: PeriodTotals, val isCurrent: Boolean)

data class YearHistory(
    val year: Int,
    val totals: PeriodTotals,
    val months: List<MonthHistory>,
    val hasPreviousYear: Boolean,
    val isCurrentYear: Boolean
)

data class WeekHistory(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totals: PeriodTotals,
    val isCurrent: Boolean
)

data class MonthDetail(val month: YearMonth, val totals: PeriodTotals, val weeks: List<WeekHistory>)

/**
 * Pure grouping behind the History screens. A movement belongs to the local date of its creation
 * in the given [ZoneId], so a late-night movement never lands in the next day, week or month because
 * of UTC. Weeks run Monday to Sunday and are clipped to the month they are shown in.
 */
object HistoryAggregator {

    private data class Dated(val date: LocalDate, val flowType: String, val amount: Double)

    /** History of [year]: movements outside it are ignored. [earliestMillis] is the oldest movement overall. */
    fun year(
        year: Int,
        movements: List<HistoryMovement>,
        earliestMillis: Long?,
        today: LocalDate,
        zone: ZoneId
    ): YearHistory {
        val inYear = dated(movements, zone).filter { it.date.year == year }
        val months = inYear
            .groupBy { YearMonth.from(it.date) }
            .map { (month, rows) -> MonthHistory(month, totals(rows), isCurrent = month == YearMonth.from(today)) }
            .sortedByDescending { it.month }
        return YearHistory(
            year = year,
            totals = totals(inYear),
            months = months,
            hasPreviousYear = earliestMillis != null && dateOf(earliestMillis, zone).year < year,
            isCurrentYear = year == today.year
        )
    }

    /**
     * Detail of [month]. Past months list every week (zero weeks included); the current month lists
     * the weeks up to the one containing [today], whole rather than cut at today.
     */
    fun month(month: YearMonth, movements: List<HistoryMovement>, today: LocalDate, zone: ZoneId): MonthDetail {
        val inMonth = dated(movements, zone).filter { YearMonth.from(it.date) == month }
        val weeks = generateSequence(month.atDay(1)) { weekEnd(it, month).plusDays(1) }
            .takeWhile { it <= month.atEndOfMonth() && it <= today }
            .map { start ->
                val end = weekEnd(start, month)
                WeekHistory(start, end, totals(inMonth.filter { it.date in start..end }), isCurrent = today in start..end)
            }
            .toList()
        return MonthDetail(month, totals(inMonth), weeks)
    }

    /** Length of a bar as a 0..1 fraction of [max]; 0 when [max] is not positive. */
    fun barFraction(value: Double, max: Double): Float =
        if (max <= 0.0) 0f else (value / max).toFloat().coerceIn(0f, 1f)

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** The Sunday closing the week of [start], or the last day of [month] when that comes first. */
    private fun weekEnd(start: LocalDate, month: YearMonth): LocalDate =
        minOf(start.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), month.atEndOfMonth())

    private fun dated(movements: List<HistoryMovement>, zone: ZoneId) =
        movements.map { Dated(dateOf(it.createdAt, zone), it.flowType, it.amount) }

    private fun totals(rows: List<Dated>) = PeriodTotals(
        income = rows.filter { it.flowType == INCOME }.sumOf { it.amount },
        expense = rows.filter { it.flowType == EXPENSE }.sumOf { it.amount },
        count = rows.size
    )

    private const val INCOME = "INCOME"
    private const val EXPENSE = "EXPENSE"
}
