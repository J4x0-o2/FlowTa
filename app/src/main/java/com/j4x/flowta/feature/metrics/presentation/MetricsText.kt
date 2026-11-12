package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.feature.history.presentation.formatMoney
import com.j4x.flowta.feature.history.presentation.monthName
import com.j4x.flowta.feature.history.presentation.weekRangeText
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriod
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType
import com.j4x.flowta.feature.metrics.domain.model.PercentChange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

// Spanish copy is spelled out here (not taken from java.time locales) so it never depends on the device or JVM locale.
private val weekdayNames = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val weekdayShort = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
private val weekdayInitials = listOf("L", "M", "M", "J", "V", "S", "D")
private val monthNames = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

private fun DayOfWeek.index() = value - 1

internal fun MetricsPeriodType.tabLabel(): String = when (this) {
    MetricsPeriodType.DAY -> "Día"
    MetricsPeriodType.WEEK -> "Semana"
    MetricsPeriodType.MONTH -> "Mes"
    MetricsPeriodType.YEAR -> "Año"
}

internal fun MetricsPeriodType.netLabel(): String = when (this) {
    MetricsPeriodType.DAY -> "Neto del día"
    MetricsPeriodType.WEEK -> "Neto de la semana"
    MetricsPeriodType.MONTH -> "Neto del mes"
    MetricsPeriodType.YEAR -> "Neto del año"
}

/** "miércoles, 11 de noviembre" */
private fun dayAndMonth(date: LocalDate) =
    "${weekdayNames[date.dayOfWeek.index()]}, ${date.dayOfMonth} de ${monthNames[date.monthValue - 1]}"

/** "Mié 11" */
internal fun shortDayLabel(date: LocalDate): String = "${weekdayShort[date.dayOfWeek.index()]} ${date.dayOfMonth}"

/** "Miércoles" */
internal fun weekdayName(date: LocalDate): String = weekdayNames[date.dayOfWeek.index()].replaceFirstChar { it.uppercase() }

/** "L" .. "D" */
internal fun weekdayInitial(date: LocalDate): String = weekdayInitials[date.dayOfWeek.index()]

private fun MetricsPeriod.lastDay(): LocalDate = endExclusive.minusDays(1)

/** Headline of the stepper: "Hoy", "Esta semana", "Noviembre 2026", "2026"... */
internal fun periodTitle(period: MetricsPeriod, today: LocalDate): String = when (period.type) {
    MetricsPeriodType.DAY -> when (period.start) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> dayAndMonth(period.start)
    }
    MetricsPeriodType.WEEK -> when (period.start) {
        MetricsPeriod.containing(MetricsPeriodType.WEEK, today).start -> "Esta semana"
        MetricsPeriod.containing(MetricsPeriodType.WEEK, today).previous().start -> "Semana pasada"
        else -> weekRangeText(period.start, period.lastDay())
    }
    MetricsPeriodType.MONTH -> "${monthName(YearMonth.from(period.start))} ${period.start.year}"
    MetricsPeriodType.YEAR -> period.start.year.toString()
}

/** Line under the title: the full date, the range, or how far along the current period is. */
internal fun periodSubtitle(period: MetricsPeriod, today: LocalDate): String {
    val current = period.isCurrent(today)
    return when (period.type) {
        MetricsPeriodType.DAY -> {
            val full = "${period.start.dayOfMonth} de ${monthNames[period.start.monthValue - 1]} de ${period.start.year}"
            if (period.start == today || period.start == today.minusDays(1)) {
                "${weekdayNames[period.start.dayOfWeek.index()]}, $full"
            } else {
                full
            }
        }
        MetricsPeriodType.WEEK -> {
            val isNamedWeek = period.start >= MetricsPeriod.containing(MetricsPeriodType.WEEK, today).previous().start &&
                period.start <= today
            when {
                current -> "${weekRangeText(period.start, period.lastDay())} · en curso"
                isNamedWeek -> weekRangeText(period.start, period.lastDay())
                else -> period.start.year.toString()
            }
        }
        MetricsPeriodType.MONTH, MetricsPeriodType.YEAR ->
            if (current) "día ${period.elapsedDays(today)} de ${period.lengthDays} · en curso" else "${period.lengthDays} días"
    }
}

/** "+$1,500", "−$1,500", or plain "$0": the net of a period with its sign. */
internal fun formatNet(value: Double, symbol: String, code: CurrencyCode): String = when {
    value > 0 -> "+${formatMoney(value, symbol, code)}"
    value < 0 -> "−${formatMoney(abs(value), symbol, code)}"
    else -> formatMoney(0.0, symbol, code)
}

/** "▲ 18%", "▼ 9%", or "0%" when nothing changed. */
internal fun PercentChange.arrowText(): String = when {
    percent > 0 -> "▲ $percent%"
    percent < 0 -> "▼ ${abs(percent)}%"
    else -> "0%"
}

/** Short amount for tight spots: "$950", "$450 mil", "$12,5 mil", "$1,18 M". */
internal fun formatCompactMoney(value: Double, symbol: String, code: CurrencyCode): String {
    val magnitude = abs(value)
    val text = when {
        magnitude >= 1_000_000 -> "${trimmed(magnitude / 1_000_000, 2)} M"
        magnitude >= 10_000 -> "${trimmed(magnitude / 1_000, 1)} mil"
        else -> return formatMoney(value, symbol, code)
    }
    return (if (value < 0) "-" else "") + symbol + text
}

/** Up to [decimals] decimals without trailing zeros, with a decimal comma. */
private fun trimmed(value: Double, decimals: Int): String =
    String.format(java.util.Locale.ROOT, "%.${decimals}f", value).trimEnd('0').trimEnd('.').replace('.', ',')
