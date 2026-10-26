package com.j4x.flowta.feature.history.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.j4x.flowta.core.domain.time.DayTicker
import com.j4x.flowta.core.domain.time.VirtualClock
import com.j4x.flowta.feature.history.domain.model.HistoryMovement
import com.j4x.flowta.feature.history.domain.repository.HistoryRepository
import com.j4x.flowta.feature.history.domain.usecase.ObserveMonthHistoryUseCase
import com.j4x.flowta.feature.history.domain.usecase.ObserveYearHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val movements = MutableStateFlow<List<HistoryMovement>>(emptyList())
    private val earliest = MutableStateFlow<Long?>(null)

    private val repository = object : HistoryRepository {
        override fun observeMovements(fromMillis: Long, toMillis: Long): Flow<List<HistoryMovement>> =
            movements.map { all -> all.filter { it.createdAt in fromMillis until toMillis } }

        override fun observeEarliest(): Flow<Long?> = earliest
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun millis(local: String): Long =
        LocalDateTime.parse(local).atZone(bogota).toInstant().toEpochMilli()

    private fun TestScope.ticker(now: String) =
        DayTicker(VirtualClock(testScheduler, java.time.Instant.ofEpochMilli(millis(now)), bogota))

    private fun TestScope.yearViewModel(now: String): HistoryViewModel =
        HistoryViewModel(SavedStateHandle(), ObserveYearHistoryUseCase(repository), ticker(now))

    private fun TestScope.advanceHours(hours: Long) {
        testScheduler.advanceTimeBy(hours * 3_600_000L)
        testScheduler.runCurrent()
    }

    // --- Year stepping bounds (R3-vm-untested) ---

    @Test
    fun nextYear_isIgnoredAtTheCurrentYear() = runTest {
        movements.value = listOf(HistoryMovement("INCOME", 10.0, millis("2026-03-05T10:00:00")))
        val vm = yearViewModel("2026-10-25T12:00:00")

        vm.state.test {
            assertEquals(2026, expectMostRecentItem().year!!.year)
            vm.onIntent(HistoryIntent.NextYear)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousYear_isIgnoredWhenThereIsNoEarlierData() = runTest {
        earliest.value = millis("2026-03-05T10:00:00")
        val vm = yearViewModel("2026-10-25T12:00:00")

        vm.state.test {
            assertFalse(expectMostRecentItem().year!!.canGoBack)
            vm.onIntent(HistoryIntent.PreviousYear)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousAndNextYear_moveWhenAllowed() = runTest {
        earliest.value = millis("2025-02-01T10:00:00")
        val vm = yearViewModel("2026-10-25T12:00:00")

        vm.state.test {
            assertEquals(2026, expectMostRecentItem().year!!.year)
            vm.onIntent(HistoryIntent.PreviousYear)
            assertEquals(2025, expectMostRecentItem().year!!.year)
            vm.onIntent(HistoryIntent.NextYear)
            assertEquals(2026, expectMostRecentItem().year!!.year)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Day rollover (R3-stale-today) ---

    @Test
    fun yearScreen_afterMidnightIntoANewYear_unlocksTheNextYearButton() = runTest {
        movements.value = listOf(HistoryMovement("INCOME", 10.0, millis("2026-12-20T10:00:00")))
        val vm = yearViewModel("2026-12-31T23:00:00")

        vm.state.test {
            val before = expectMostRecentItem().year!!
            assertEquals(2026, before.year)
            assertFalse(before.canGoForward)
            assertTrue(before.months.single().isCurrent)

            advanceHours(2)

            val after = expectMostRecentItem().year!!
            assertEquals(2026, after.year)
            assertTrue(after.canGoForward)
            assertFalse(after.months.single().isCurrent)

            vm.onIntent(HistoryIntent.NextYear)
            assertEquals(2027, expectMostRecentItem().year!!.year)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun yearScreen_afterMidnightIntoANewMonth_movesTheCurrentMarker() = runTest {
        movements.value = listOf(
            HistoryMovement("INCOME", 10.0, millis("2026-10-20T10:00:00")),
            HistoryMovement("EXPENSE", 4.0, millis("2026-11-01T09:00:00"))
        )
        val vm = yearViewModel("2026-10-31T22:00:00")

        vm.state.test {
            val before = expectMostRecentItem().year!!
            assertEquals(listOf(YearMonth.of(2026, 10)), before.months.filter { it.isCurrent }.map { it.month })

            advanceHours(3)

            val after = expectMostRecentItem().year!!
            assertEquals(listOf(YearMonth.of(2026, 11)), after.months.filter { it.isCurrent }.map { it.month })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun monthScreen_afterMidnight_includesTheNewCurrentWeek() = runTest {
        // Sunday Oct 25 to Monday Oct 26: a new week starts.
        movements.value = listOf(HistoryMovement("INCOME", 10.0, millis("2026-10-20T10:00:00")))
        val handle = SavedStateHandle(mapOf("year" to 2026, "month" to 10))
        val vm = HistoryMonthViewModel(handle, ObserveMonthHistoryUseCase(repository), ticker("2026-10-25T22:00:00"))

        vm.state.test {
            val before = expectMostRecentItem().month!!
            assertEquals("19 – 25 oct", before.weeks.first().rangeText)

            advanceHours(3)

            val after = expectMostRecentItem().month!!
            assertEquals("26 – 31 oct", after.weeks.first().rangeText)
            assertTrue(after.weeks.first().isCurrent)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
