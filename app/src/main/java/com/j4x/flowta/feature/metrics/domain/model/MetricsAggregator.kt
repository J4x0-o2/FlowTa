package com.j4x.flowta.feature.metrics.domain.model

import com.j4x.flowta.feature.history.domain.model.HistoryMovement
import com.j4x.flowta.feature.history.domain.model.PeriodTotals
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/** Income and expense of one local day. */
data class DayTotals(val date: LocalDate, val income: Double, val expense: Double) {
    val net: Double get() = income - expense
}

/** Whole-percent variation against a previous value; positive means it went up. */
data class PercentChange(val percent: Int)

/** A category's share of the period's expenses; [barFraction] is relative to the top category. */
data class CategoryShare(val category: CategoryExpense, val percent: Int, val barFraction: Float)

/** An income source's share of the period's income; [id] is null for income without a source. */
data class IncomeSourceShare(
    val id: String?,
    val name: String,
    val total: Double,
    val percent: Int,
    val barFraction: Float
)

/** Everything Metrics shows for one [period]. Transfers are never part of it. */
data class PeriodMetrics(
    val period: MetricsPeriod,
    val totals: PeriodTotals,
    val previous: PeriodTotals,
    /** One entry per day of the period, in order, including days without movements. */
    val days: List<DayTotals>,
    /** Expense categories with a positive total, biggest first. */
    val categories: List<CategoryExpense>,
    /** Days of the period elapsed up to today (the whole period once it is over). */
    val elapsedDays: Int,
    /** Whether any movement exists before the period starts. */
    val hasPrevious: Boolean,
    /** Income by source (Month and Year only), biggest first. */
    val incomeSources: List<IncomeSourceShare> = emptyList()
) {
    val income: Double get() = totals.income
    val expense: Double get() = totals.expense
    val net: Double get() = totals.net

    val netChange: PercentChange? get() = MetricsAggregator.percentChange(net, previous.net)
    val incomeChange: PercentChange? get() = MetricsAggregator.percentChange(income, previous.income)
    val expenseChange: PercentChange? get() = MetricsAggregator.percentChange(expense, previous.expense)

    val averageDailyExpense: Double get() = if (elapsedDays <= 0) 0.0 else expense / elapsedDays

    /** The day with the highest expenses (the earliest on a tie); null when nothing was spent. */
    val mostExpensiveDay: DayTotals?
        get() = days.fold(null as DayTotals?) { best, day ->
            if (day.expense > 0.0 && (best == null || day.expense > best.expense)) day else best
        }

    /** Income left after expenses as a whole percent; null when there was no income. */
    val savingsPercent: Int? get() = MetricsAggregator.savingsPercent(income, expense)
}

/**
 * Pure calculations behind Metrics. A movement belongs to the local date of its creation in the given
 * [ZoneId], like the History screens, so a late-night movement never lands in the next day because of UTC.
 */
object MetricsAggregator {

    /**
     * Metrics of [period]. [movements] should cover the previous period too (its totals feed the
     * deltas); movements outside both are ignored. [earliestMillis] is the oldest movement overall.
     */
    fun compute(
        period: MetricsPeriod,
        movements: List<HistoryMovement>,
        categories: List<CategoryExpense>,
        earliestMillis: Long?,
        today: LocalDate,
        zone: ZoneId,
        incomeSources: List<IncomeSourceTotal> = emptyList()
    ): PeriodMetrics {
        val previousPeriod = period.previous()
        val inPeriod = mutableListOf<HistoryMovement>()
        val inPrevious = mutableListOf<HistoryMovement>()
        val byDate = HashMap<LocalDate, MutableList<HistoryMovement>>()
        movements.forEach {
            val date = dateOf(it.createdAt, zone)
            when {
                date in period -> {
                    inPeriod += it
                    byDate.getOrPut(date) { mutableListOf() } += it
                }
                date in previousPeriod -> inPrevious += it
            }
        }
        val days = generateSequence(period.start) { it.plusDays(1) }
            .take(period.lengthDays)
            .map { date -> dayTotals(date, byDate[date].orEmpty()) }
            .toList()
        return PeriodMetrics(
            period = period,
            totals = totals(inPeriod),
            previous = totals(inPrevious),
            days = days,
            categories = categories.filter { it.total > 0 }.sortedByDescending { it.total },
            elapsedDays = period.elapsedDays(today),
            hasPrevious = earliestMillis != null && dateOf(earliestMillis, zone) < period.start,
            incomeSources = incomeSourceShares(incomeSources)
        )
    }

    /** Variation of [current] against [previous], by its magnitude; null when [previous] is zero. */
    fun percentChange(current: Double, previous: Double): PercentChange? =
        if (previous == 0.0) null else PercentChange(((current - previous) / abs(previous) * 100).roundToInt())

    /** Share of income left after expenses, whole percent; negative when overspent, null without income. */
    fun savingsPercent(income: Double, expense: Double): Int? =
        if (income <= 0.0) null else ((income - expense) / income * 100).roundToInt()

    /** Percent of the total and bar length relative to the biggest category, for categories in order. */
    fun categoryShares(categories: List<CategoryExpense>): List<CategoryShare> {
        val total = categories.sumOf { it.total }
        val top = categories.maxOfOrNull { it.total } ?: return emptyList()
        if (total <= 0.0 || top <= 0.0) return emptyList()
        return categories.map {
            CategoryShare(it, (it.total / total * 100).roundToInt(), (it.total / top).toFloat().coerceIn(0f, 1f))
        }
    }

    /**
     * Income per source with its percent of the total and bar length relative to the biggest one.
     * Rows without a source (or whose source type no longer exists) merge into "Sin fuente".
     */
    fun incomeSourceShares(rows: List<IncomeSourceTotal>): List<IncomeSourceShare> {
        val merged = LinkedHashMap<String?, Pair<String, Double>>()
        rows.forEach { row ->
            val named = row.sourceId != null && row.sourceName != null
            val key = if (named) row.sourceId else null
            val name = if (named) row.sourceName!! else NO_SOURCE_NAME
            merged[key] = name to ((merged[key]?.second ?: 0.0) + row.total)
        }
        val positive = merged.entries.filter { it.value.second > 0.0 }.sortedByDescending { it.value.second }
        val total = positive.sumOf { it.value.second }
        val top = positive.firstOrNull()?.value?.second ?: return emptyList()
        return positive.map { (id, value) ->
            IncomeSourceShare(
                id, value.first, value.second,
                (value.second / total * 100).roundToInt(),
                (value.second / top).toFloat().coerceIn(0f, 1f)
            )
        }
    }

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    private fun dayTotals(date: LocalDate, rows: List<HistoryMovement>) = DayTotals(
        date = date,
        income = rows.filter { it.flowType == INCOME }.sumOf { it.amount },
        expense = rows.filter { it.flowType == EXPENSE }.sumOf { it.amount }
    )

    private fun totals(rows: List<HistoryMovement>) = PeriodTotals(
        income = rows.filter { it.flowType == INCOME }.sumOf { it.amount },
        expense = rows.filter { it.flowType == EXPENSE }.sumOf { it.amount },
        count = rows.size
    )

    const val NO_SOURCE_NAME = "Sin fuente"
    private const val INCOME = "INCOME"
    private const val EXPENSE = "EXPENSE"
}
