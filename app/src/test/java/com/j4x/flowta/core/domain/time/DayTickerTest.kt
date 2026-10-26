package com.j4x.flowta.core.domain.time

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayTickerTest {

    private val bogota = ZoneId.of("America/Bogota")

    private fun instant(local: String): Instant = LocalDateTime.parse(local).atZone(bogota).toInstant()

    @Test
    fun today_isTheLocalDateOfTheClock() = runTest {
        // 02:00 UTC on Jan 1 is still Dec 31 in Bogota (UTC-5).
        val ticker = DayTicker(VirtualClock(testScheduler, Instant.parse("2027-01-01T02:00:00Z"), bogota))

        assertEquals(LocalDate.of(2026, 12, 31), ticker.today())
    }

    @Test
    fun days_emitsTodayFirstAndTheNextDateAtLocalMidnight() = runTest {
        val ticker = DayTicker(VirtualClock(testScheduler, instant("2026-12-31T23:59:00"), bogota))

        ticker.days().test {
            assertEquals(LocalDate.of(2026, 12, 31), awaitItem())
            testScheduler.advanceTimeBy(59_000)
            testScheduler.runCurrent()
            expectNoEvents()
            testScheduler.advanceTimeBy(1_001)
            testScheduler.runCurrent()
            assertEquals(LocalDate.of(2027, 1, 1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun days_keepsTickingEveryDay() = runTest {
        val ticker = DayTicker(VirtualClock(testScheduler, instant("2026-10-25T12:00:00"), bogota))

        ticker.days().test {
            assertEquals(LocalDate.of(2026, 10, 25), awaitItem())
            testScheduler.advanceTimeBy(12 * 3_600_000L + 1_000)
            testScheduler.runCurrent()
            assertEquals(LocalDate.of(2026, 10, 26), awaitItem())
            testScheduler.advanceTimeBy(24 * 3_600_000L)
            testScheduler.runCurrent()
            assertEquals(LocalDate.of(2026, 10, 27), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
