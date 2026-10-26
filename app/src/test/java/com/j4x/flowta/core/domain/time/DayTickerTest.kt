package com.j4x.flowta.core.domain.time

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
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

    /** A clock whose zone and offset from the virtual time can be changed mid-test. */
    private class MovableClock(private val scheduler: TestCoroutineScheduler, start: Instant, var zone0: ZoneId) : Clock() {
        var skewMillis = 0L
        private val startInstant = start
        override fun getZone(): ZoneId = zone0
        override fun withZone(zone: ZoneId): Clock = MovableClock(scheduler, startInstant, zone)
        override fun instant(): Instant = startInstant.plusMillis(scheduler.currentTime + skewMillis)
    }

    private class FakeSignal : TimeChangeSignal {
        val source = MutableSharedFlow<Unit>()
        override val changes: Flow<Unit> = source
    }

    @Test
    fun days_reEvaluatesInTheNewZone_whenTheTimeChangeSignalFires() = runTest {
        val clock = MovableClock(testScheduler, instant("2026-10-25T22:00:00"), bogota)
        val signal = FakeSignal()

        DayTicker(clock, signal).days().test {
            assertEquals(LocalDate.of(2026, 10, 25), awaitItem())

            clock.zone0 = ZoneId.of("Asia/Tokyo") // 22:00 in Bogota is already Oct 26 in Tokyo
            signal.source.emit(Unit)
            testScheduler.runCurrent() // no virtual time passes: only the signal can explain a new date

            assertEquals(LocalDate.of(2026, 10, 26), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun days_reEvaluatesAfterAClockJump_andKeepsTickingFromTheNewMidnight() = runTest {
        val clock = MovableClock(testScheduler, instant("2026-10-25T12:00:00"), bogota)
        val signal = FakeSignal()

        DayTicker(clock, signal).days().test {
            assertEquals(LocalDate.of(2026, 10, 25), awaitItem())

            clock.skewMillis = 24 * 3_600_000L // user sets the date one day ahead
            signal.source.emit(Unit)
            testScheduler.runCurrent()
            assertEquals(LocalDate.of(2026, 10, 26), expectMostRecentItem())

            testScheduler.advanceTimeBy(12 * 3_600_000L + 1_000) // timer was rescheduled for the new midnight
            testScheduler.runCurrent()
            assertEquals(LocalDate.of(2026, 10, 27), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun days_doesNotRepeatTheSameDate_whenTheSignalFiresWithoutAChange() = runTest {
        val clock = MovableClock(testScheduler, instant("2026-10-25T12:00:00"), bogota)
        val signal = FakeSignal()

        DayTicker(clock, signal).days().test {
            assertEquals(LocalDate.of(2026, 10, 25), awaitItem())
            signal.source.emit(Unit)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
