package com.j4x.flowta.feature.history.presentation

import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.format.Formatters
import com.j4x.flowta.feature.history.domain.model.HistoryAggregator
import com.j4x.flowta.feature.history.domain.model.MonthDetail
import com.j4x.flowta.feature.history.domain.model.PeriodTotals
import com.j4x.flowta.feature.history.domain.model.WeekHistory
import com.j4x.flowta.feature.history.domain.model.YearHistory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val spanish = Locale.forLanguageTag("es")
private val monthNameFormat = DateTimeFormatter.ofPattern("MMMM", spanish)
private val monthShort = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** "Octubre": the Spanish name of [month], first letter in uppercase. */
internal fun monthName(month: YearMonth): String =
    monthNameFormat.format(month).replaceFirstChar { it.titlecase(spanish) }

/** "1 – 4 oct", "31 oct" for a one day week, "29 sep – 5 oct" across two months. */
internal fun weekRangeText(start: LocalDate, end: LocalDate): String {
    val endText = "${end.dayOfMonth} ${monthShort[end.monthValue - 1]}"
    return when {
        start == end -> endText
        start.month == end.month -> "${start.dayOfMonth} – $endText"
        else -> "${start.dayOfMonth} ${monthShort[start.monthValue - 1]} – $endText"
    }
}

/** "$1,250", or "-$1,250" for a negative [value]. */
fun formatMoney(value: Double, symbol: String, code: CurrencyCode): String {
    val text = "$symbol${Formatters.amount(abs(value), code)}"
    return if (value < 0) "-$text" else text
}

/** "+$500"; plain "$0" for zero. */
fun formatIncome(value: Double, symbol: String, code: CurrencyCode): String =
    if (value == 0.0) formatMoney(0.0, symbol, code) else "+${formatMoney(value, symbol, code)}"

/** "−$500"; plain "$0" for zero. */
fun formatExpense(value: Double, symbol: String, code: CurrencyCode): String =
    if (value == 0.0) formatMoney(0.0, symbol, code) else "−${formatMoney(value, symbol, code)}"

/** The best single income or expense value among the siblings: the length every bar is scaled to. */
private fun best(all: List<PeriodTotals>): Double = all.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0

data class MonthRowModel(
    val month: YearMonth,
    val name: String,
    /** The month of today: the card reads "Octubre · en curso". */
    val isCurrent: Boolean,
    val income: Double,
    val expense: Double,
    val net: Double,
    val incomeFraction: Float,
    val expenseFraction: Float
)

data class YearHistoryModel(
    val year: Int,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val income: Double,
    val expense: Double,
    val net: Double,
    val months: List<MonthRowModel>
) {
    val isEmpty: Boolean get() = months.isEmpty()

    companion object {
        fun from(history: YearHistory): YearHistoryModel {
            val best = best(history.months.map { it.totals })
            return YearHistoryModel(
                year = history.year,
                canGoBack = history.hasPreviousYear,
                canGoForward = !history.isCurrentYear,
                income = history.totals.income,
                expense = history.totals.expense,
                net = history.totals.net,
                months = history.months.map {
                    MonthRowModel(
                        month = it.month,
                        name = monthName(it.month),
                        isCurrent = it.isCurrent,
                        income = it.totals.income,
                        expense = it.totals.expense,
                        net = it.totals.net,
                        incomeFraction = HistoryAggregator.barFraction(it.totals.income, best),
                        expenseFraction = HistoryAggregator.barFraction(it.totals.expense, best)
                    )
                }
            )
        }
    }
}

data class WeekRowModel(
    val rangeText: String,
    val isCurrent: Boolean,
    val income: Double,
    val expense: Double,
    val net: Double,
    val incomeFraction: Float,
    val expenseFraction: Float
)

data class MonthHistoryModel(
    val month: YearMonth,
    val title: String,
    val year: Int,
    val income: Double,
    val expense: Double,
    val net: Double,
    val count: Int,
    /** Newest week first. */
    val weeks: List<WeekRowModel>
) {
    companion object {
        fun from(detail: MonthDetail): MonthHistoryModel {
            val best = best(detail.weeks.map { it.totals })
            return MonthHistoryModel(
                month = detail.month,
                title = monthName(detail.month),
                year = detail.month.year,
                income = detail.totals.income,
                expense = detail.totals.expense,
                net = detail.totals.net,
                count = detail.totals.count,
                weeks = detail.weeks.map { row(it, best) }.asReversed()
            )
        }

        private fun row(week: WeekHistory, best: Double) = WeekRowModel(
            rangeText = weekRangeText(week.startDate, week.endDate),
            isCurrent = week.isCurrent,
            income = week.totals.income,
            expense = week.totals.expense,
            net = week.totals.net,
            incomeFraction = HistoryAggregator.barFraction(week.totals.income, best),
            expenseFraction = HistoryAggregator.barFraction(week.totals.expense, best)
        )
    }
}
