package com.j4x.flowta.core.presentation.format

import com.j4x.flowta.core.domain.model.CurrencyCode
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    private val utc = ZoneId.of("UTC")
    // 2026-05-29T14:55:00Z (a Friday)
    private val sample = 1_780_066_500_000L

    @Test
    fun amount_cop_groupsThousandsWithCommasAndTruncates() {
        assertEquals("1,234,567", Formatters.amount(1_234_567.89, CurrencyCode.COP))
        assertEquals("0", Formatters.amount(0.0, CurrencyCode.COP))
        assertEquals("999", Formatters.amount(999.99, CurrencyCode.COP))
    }

    @Test
    fun amount_otherCurrencies_useTwoDecimalsLikeLegacyFormat() {
        assertEquals("%.2f".format(1234.5), Formatters.amount(1234.5, CurrencyCode.USD))
        assertEquals("%.2f".format(0.005), Formatters.amount(0.005, CurrencyCode.EUR))
    }

    @Test
    fun bubbleDate_matchesExpectedShape() {
        assertEquals("29 may · 2:55 PM", Formatters.bubbleDate(sample, utc))
    }

    @Test
    fun detailDate_matchesExpectedShape() {
        assertEquals("viernes 29 de mayo, 2026 · 2:55 PM", Formatters.detailDate(sample, utc))
    }

    @Test
    fun dates_matchLegacySimpleDateFormatOutputAcrossTimestampsAndZones() {
        val stamps = listOf(sample, 0L, 1_704_067_200_000L, 1_735_689_599_000L, 1_751_328_000_000L)
        for (zoneId in listOf("UTC", "America/Bogota", "Europe/Madrid")) {
            val tz = TimeZone.getTimeZone(zoneId)
            val zone = ZoneId.of(zoneId)
            for (ts in stamps) {
                val day = SimpleDateFormat("d MMM", Locale("es")).apply { timeZone = tz }.format(Date(ts))
                val time = SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { timeZone = tz }.format(Date(ts))
                assertEquals("$day · $time", Formatters.bubbleDate(ts, zone))
                val long = SimpleDateFormat("EEEE d 'de' MMMM, yyyy", Locale("es")).apply { timeZone = tz }.format(Date(ts))
                assertEquals("$long · $time", Formatters.detailDate(ts, zone))
            }
        }
    }

    @Test
    fun amount_isSafeUnderConcurrentUse() {
        val pool = Executors.newFixedThreadPool(8)
        val futures = (1..200).map { i ->
            pool.submit<String> { Formatters.amount(i * 1000.0, CurrencyCode.COP) }
        }
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()
        results.forEachIndexed { idx, s ->
            val n = (idx + 1) * 1000L
            assertEquals(java.text.DecimalFormat("#,##0", java.text.DecimalFormatSymbols(Locale.US)).format(n), s)
        }
    }
}
