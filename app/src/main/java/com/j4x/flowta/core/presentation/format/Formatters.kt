package com.j4x.flowta.core.presentation.format

import com.j4x.flowta.core.domain.model.CurrencyCode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared, cached formatters. Never allocate DecimalFormat/DateTimeFormatter per call or
 * per recomposition; use these instead.
 */
object Formatters {

    // DecimalFormat is NOT thread-safe, so each thread gets its own instance.
    private val copFormat = ThreadLocal.withInitial {
        DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    }

    // DateTimeFormatter is immutable and thread-safe. The zone is applied per call (not baked
    // into the formatter) so a device time zone change is always respected.
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
    private val longDayFormatter  = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy", Locale("es"))
    private val timeFormatter     = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /** COP: truncated, comma-grouped integer. Others: two decimals (default locale, as before). */
    fun amount(value: Double, code: CurrencyCode): String = when (code) {
        CurrencyCode.COP -> copFormat.get()!!.format(value.toLong())
        else             -> "%.2f".format(value)
    }

    /** e.g. "29 may · 2:55 PM" */
    fun bubbleDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return "${dayMonthFormatter.format(at)} · ${timeFormatter.format(at)}"
    }

    /**
     * Time span of a grouped bubble. Same day: "29 may · 9:15 AM – 5:40 PM";
     * spanning days: "29 may 9:15 AM – 30 may 8:00 AM".
     */
    fun bubbleRange(fromMillis: Long, toMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val from = Instant.ofEpochMilli(fromMillis).atZone(zone)
        val to   = Instant.ofEpochMilli(toMillis).atZone(zone)
        return if (from.toLocalDate() == to.toLocalDate()) {
            "${dayMonthFormatter.format(from)} · ${timeFormatter.format(from)} – ${timeFormatter.format(to)}"
        } else {
            "${dayMonthFormatter.format(from)} ${timeFormatter.format(from)} – " +
                "${dayMonthFormatter.format(to)} ${timeFormatter.format(to)}"
        }
    }

    /** e.g. "viernes 29 de mayo, 2026 · 2:55 PM" */
    fun detailDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return "${longDayFormatter.format(at)} · ${timeFormatter.format(at)}"
    }
}
