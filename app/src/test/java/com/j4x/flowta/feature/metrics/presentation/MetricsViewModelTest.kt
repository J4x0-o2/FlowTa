package com.j4x.flowta.feature.metrics.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.j4x.flowta.core.domain.time.DayTicker
import com.j4x.flowta.core.domain.time.VirtualClock
import com.j4x.flowta.feature.history.domain.model.HistoryMovement
import com.j4x.flowta.feature.history.domain.repository.HistoryRepository
import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType
import com.j4x.flowta.feature.metrics.domain.repository.MetricsRepository
import com.j4x.flowta.feature.metrics.domain.usecase.ObserveMetricsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsViewModelTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val movements = MutableStateFlow<List<HistoryMovement>>(emptyList())
    private val earliest = MutableStateFlow<Long?>(null)
    private val categories = MutableStateFlow<List<CategoryExpense>>(emptyList())
    private val requestedCategoryRanges = mutableListOf<DateRange>()

    private val history = object : HistoryRepository {
        override fun observeMovements(fromMillis: Long, toMillis: Long): Flow<List<HistoryMovement>> =
            movements.map { all -> all.filter { it.createdAt in fromMillis until toMillis } }

        override fun observeEarliest(): Flow<Long?> = earliest
    }

    private val metrics = object : MetricsRepository {
        override fun getExpensesByCategory(range: DateRange): Flow<List<CategoryExpense>> {
            requestedCategoryRanges += range
            return categories
        }

        override fun getNetBalance(): Flow<Double> = emptyFlow()
        override fun getTotalTransfers(range: DateRange): Flow<Double> = emptyFlow()
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun millis(local: String): Long =
        LocalDateTime.parse(local).atZone(bogota).toInstant().toEpochMilli()

    private fun TestScope.viewModel(
        now: String = "2026-11-11T12:00:00", // Wednesday
        handle: SavedStateHandle = SavedStateHandle()
    ) = MetricsViewModel(
        handle,
        ObserveMetricsUseCase(history, metrics),
        DayTicker(VirtualClock(testScheduler, Instant.ofEpochMilli(millis(now)), bogota))
    )

    private fun TestScope.advanceHours(hours: Long) {
        testScheduler.advanceTimeBy(hours * 3_600_000L)
        testScheduler.runCurrent()
    }

    private fun date(text: String) = LocalDate.parse(text)

    @Test
    fun startsOnTodayDayTabWithItsNet() = runTest {
        movements.value = listOf(
            HistoryMovement("INCOME", 100.0, millis("2026-11-11T09:00:00")),
            HistoryMovement("EXPENSE", 30.0, millis("2026-11-11T10:00:00")),
            HistoryMovement("EXPENSE", 99.0, millis("2026-11-10T10:00:00"))
        )
        val vm = viewModel()

        vm.state.test {
            val state = expectMostRecentItem()
            assertEquals(MetricsPeriodType.DAY, state.periodType)
            assertEquals(date("2026-11-11"), state.period!!.start)
            assertEquals(70.0, state.metrics!!.net, 0.0)
            assertFalse(state.canGoForward)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectingWeek_showsTheCalendarWeekAndSelectsToday() = runTest {
        movements.value = listOf(HistoryMovement("EXPENSE", 30.0, millis("2026-11-09T10:00:00")))
        val vm = viewModel()

        vm.state.test {
            expectMostRecentItem()
            vm.onIntent(MetricsIntent.SelectType(MetricsPeriodType.WEEK))
            val state = expectMostRecentItem()
            assertEquals(date("2026-11-09"), state.period!!.start)
            assertEquals(7, state.metrics!!.days.size)
            assertEquals(30.0, state.metrics!!.expense, 0.0)
            assertEquals(date("2026-11-11"), state.selectedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun weekCategories_areQueriedForTheWeekRangeInclusiveOfItsLastMillisecond() = runTest {
        categories.value = listOf(CategoryExpense("food", "Comida", "EXPENSE", 30.0))
        val vm = viewModel()

        vm.state.test {
            vm.onIntent(MetricsIntent.SelectType(MetricsPeriodType.WEEK))
            val state = expectMostRecentItem()
            assertEquals(listOf("food"), state.metrics!!.categories.map { it.typeId })
            val range = requestedCategoryRanges.last()
            assertEquals(millis("2026-11-09T00:00:00"), range.from)
            assertEquals(millis("2026-11-16T00:00:00") - 1, range.to)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectDay_highlightsItAndIgnoresDaysOutsideThePeriod() = runTest {
        val vm = viewModel()

        vm.state.test {
            vm.onIntent(MetricsIntent.SelectType(MetricsPeriodType.WEEK))
            vm.onIntent(MetricsIntent.SelectDay(date("2026-11-10")))
            assertEquals(date("2026-11-10"), expectMostRecentItem().selectedDay)
            vm.onIntent(MetricsIntent.SelectDay(date("2026-11-20")))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pastWeek_hasNoDefaultSelectedDay_andNavigatingClearsThePickedOne() = runTest {
        earliest.value = millis("2026-10-01T10:00:00")
        val vm = viewModel()

        vm.state.test {
            vm.onIntent(MetricsIntent.SelectType(MetricsPeriodType.WEEK))
            vm.onIntent(MetricsIntent.SelectDay(date("2026-11-10")))
            vm.onIntent(MetricsIntent.Previous)
            assertNull(expectMostRecentItem().selectedDay)
            vm.onIntent(MetricsIntent.Next)
            assertEquals(date("2026-11-11"), expectMostRecentItem().selectedDay)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun next_isIgnoredAtTheCurrentPeriod() = runTest {
        val vm = viewModel()

        vm.state.test {
            expectMostRecentItem()
            vm.onIntent(MetricsIntent.Next)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previous_isIgnoredWhenNothingHappenedBefore() = runTest {
        earliest.value = millis("2026-11-11T08:00:00")
        val vm = viewModel()

        vm.state.test {
            assertFalse(expectMostRecentItem().canGoBack)
            vm.onIntent(MetricsIntent.Previous)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun previousAndNext_walkThePeriodsAndStopAtTheEarliestMovement() = runTest {
        earliest.value = millis("2026-11-09T08:00:00") // Monday of the current week
        val vm = viewModel()

        vm.state.test {
            vm.onIntent(MetricsIntent.SelectType(MetricsPeriodType.DAY))
            assertTrue(expectMostRecentItem().canGoBack)
            vm.onIntent(MetricsIntent.Previous)
            assertEquals(date("2026-11-10"), expectMostRecentItem().period!!.start)
            vm.onIntent(MetricsIntent.Previous)
            val monday = expectMostRecentItem()
            assertEquals(date("2026-11-09"), monday.period!!.start)
            assertFalse(monday.canGoBack)
            vm.onIntent(MetricsIntent.Previous)
            expectNoEvents()
            vm.onIntent(MetricsIntent.Next)
            assertEquals(date("2026-11-10"), expectMostRecentItem().period!!.start)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun switchingTab_returnsToTheCurrentPeriod() = runTest {
        earliest.value = millis("2026-01-01T08:00:00")
        val vm = viewModel()

        vm.state.test {
            vm.onIntent(MetricsIntent.Previous)
            vm.onIntent(MetricsIntent.Previous)
            vm.onIntent(MetricsIntent.SelectType(MetricsPeriodType.MONTH))
            val state = expectMostRecentItem()
            assertEquals(date("2026-11-01"), state.period!!.start)
            assertFalse(state.canGoForward)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selection_isRestoredFromTheSavedState() = runTest {
        earliest.value = millis("2026-01-01T08:00:00")
        val handle = SavedStateHandle(mapOf("type" to "WEEK", "offset" to 1))
        val vm = viewModel(handle = handle)

        vm.state.test {
            val state = expectMostRecentItem()
            assertEquals(MetricsPeriodType.WEEK, state.periodType)
            assertEquals(date("2026-11-02"), state.period!!.start)
            assertTrue(state.canGoForward)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun anUnknownSavedType_fallsBackToDay() = runTest {
        val vm = viewModel(handle = SavedStateHandle(mapOf("type" to "CENTURY")))

        vm.state.test {
            assertEquals(MetricsPeriodType.DAY, expectMostRecentItem().periodType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun afterMidnight_theCurrentPeriodMovesOn() = runTest {
        movements.value = listOf(HistoryMovement("EXPENSE", 5.0, millis("2026-11-12T01:00:00")))
        val vm = viewModel(now = "2026-11-11T22:00:00")

        vm.state.test {
            val before = expectMostRecentItem()
            assertEquals(date("2026-11-11"), before.period!!.start)
            assertEquals(0.0, before.metrics!!.expense, 0.0)

            advanceHours(3)

            val after = expectMostRecentItem()
            assertEquals(date("2026-11-12"), after.period!!.start)
            assertEquals(5.0, after.metrics!!.expense, 0.0)
            assertEquals(date("2026-11-12"), after.today)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
