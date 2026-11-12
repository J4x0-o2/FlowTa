package com.j4x.flowta.feature.metrics.domain.model

import com.j4x.flowta.feature.history.domain.model.HistoryMovement
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.DAY
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.WEEK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MetricsAggregatorTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val today = LocalDate.parse("2026-11-11") // Wednesday
    private val week = MetricsPeriod.containing(WEEK, today)

    private fun at(local: String) = LocalDateTime.parse(local).atZone(bogota).toInstant().toEpochMilli()
    private fun income(amount: Double, local: String) = HistoryMovement("INCOME", amount, at(local))
    private fun expense(amount: Double, local: String) = HistoryMovement("EXPENSE", amount, at(local))

    private fun compute(
        movements: List<HistoryMovement>,
        period: MetricsPeriod = week,
        categories: List<CategoryExpense> = emptyList(),
        earliest: Long? = null,
        now: LocalDate = today
    ) = MetricsAggregator.compute(period, movements, categories, earliest, now, bogota)

    // --- week aggregation ---

    @Test
    fun week_hasSevenDaysMondayToSundayEvenWithoutMovements() {
        val metrics = compute(emptyList())
        assertEquals(7, metrics.days.size)
        assertEquals(LocalDate.parse("2026-11-09"), metrics.days.first().date)
        assertEquals(LocalDate.parse("2026-11-15"), metrics.days.last().date)
        assertTrue(metrics.days.all { it.income == 0.0 && it.expense == 0.0 })
    }

    @Test
    fun week_groupsMovementsByLocalDateNotUtc() {
        // 23:30 in Bogota on Tuesday is already Wednesday in UTC.
        val metrics = compute(listOf(expense(10.0, "2026-11-10T23:30:00"), income(50.0, "2026-11-11T00:10:00")))
        assertEquals(10.0, metrics.days[1].expense, 0.0)
        assertEquals(0.0, metrics.days[2].expense, 0.0)
        assertEquals(50.0, metrics.days[2].income, 0.0)
    }

    @Test
    fun week_totalsOnlyCountTheSelectedPeriod() {
        val metrics = compute(
            listOf(
                income(100.0, "2026-11-09T08:00:00"),
                expense(30.0, "2026-11-11T12:00:00"),
                expense(5.0, "2026-11-11T18:00:00"),
                income(999.0, "2026-11-16T00:00:00"), // next Monday
                expense(777.0, "2026-11-08T23:59:00") // previous Sunday
            )
        )
        assertEquals(100.0, metrics.income, 0.0)
        assertEquals(35.0, metrics.expense, 0.0)
        assertEquals(65.0, metrics.net, 0.0)
        assertEquals(35.0, metrics.days[2].expense, 0.0)
    }

    @Test
    fun previousTotals_comeFromThePreviousPeriod() {
        val metrics = compute(
            listOf(income(40.0, "2026-11-04T10:00:00"), expense(15.0, "2026-11-05T10:00:00"), income(100.0, "2026-11-10T10:00:00"))
        )
        assertEquals(40.0, metrics.previous.income, 0.0)
        assertEquals(15.0, metrics.previous.expense, 0.0)
        assertEquals(25.0, metrics.previous.net, 0.0)
        assertEquals(100.0, metrics.income, 0.0)
    }

    @Test
    fun day_aggregatesASingleDay() {
        val day = MetricsPeriod.containing(DAY, today)
        val metrics = compute(
            listOf(income(100.0, "2026-11-11T09:00:00"), expense(30.0, "2026-11-11T20:00:00"), expense(8.0, "2026-11-10T20:00:00")),
            period = day
        )
        assertEquals(1, metrics.days.size)
        assertEquals(70.0, metrics.net, 0.0)
        assertEquals(-8.0, metrics.previous.net, 0.0)
    }

    @Test
    fun hasPrevious_isTrueOnlyWhenSomethingHappenedBeforeThePeriod() {
        assertFalse(compute(emptyList(), earliest = null).hasPrevious)
        assertFalse(compute(emptyList(), earliest = at("2026-11-09T00:00:00")).hasPrevious)
        assertTrue(compute(emptyList(), earliest = at("2026-11-08T23:59:00")).hasPrevious)
    }

    // --- percent change ---

    @Test
    fun percentChange_isNullWhenThePreviousIsZero() {
        assertNull(MetricsAggregator.percentChange(100.0, 0.0))
        assertNull(MetricsAggregator.percentChange(0.0, 0.0))
    }

    @Test
    fun percentChange_risesAndFalls() {
        assertEquals(PercentChange(18), MetricsAggregator.percentChange(118.0, 100.0))
        assertEquals(PercentChange(-50), MetricsAggregator.percentChange(50.0, 100.0))
        assertEquals(PercentChange(0), MetricsAggregator.percentChange(100.0, 100.0))
        assertEquals(PercentChange(200), MetricsAggregator.percentChange(300.0, 100.0))
    }

    @Test
    fun percentChange_withNegativePreviousUsesItsMagnitude() {
        // Net went from -100 to -50: an improvement of 50%.
        assertEquals(PercentChange(50), MetricsAggregator.percentChange(-50.0, -100.0))
        assertEquals(PercentChange(-150), MetricsAggregator.percentChange(-50.0, 100.0))
    }

    @Test
    fun percentChange_roundsToTheNearestWholePercent() {
        assertEquals(PercentChange(9), MetricsAggregator.percentChange(109.4, 100.0))
        assertEquals(PercentChange(10), MetricsAggregator.percentChange(109.6, 100.0))
    }

    @Test
    fun deltas_useNetIncomeAndExpenseAgainstThePreviousPeriod() {
        val metrics = compute(
            listOf(
                income(100.0, "2026-11-04T10:00:00"), expense(50.0, "2026-11-05T10:00:00"), // previous: net 50
                income(200.0, "2026-11-10T10:00:00"), expense(100.0, "2026-11-11T10:00:00") // current: net 100
            )
        )
        assertEquals(PercentChange(100), metrics.incomeChange)
        assertEquals(PercentChange(100), metrics.expenseChange)
        assertEquals(PercentChange(100), metrics.netChange)
    }

    @Test
    fun deltas_areNullWithoutPreviousActivity() {
        val metrics = compute(listOf(income(200.0, "2026-11-10T10:00:00")))
        assertNull(metrics.incomeChange)
        assertNull(metrics.expenseChange)
        assertNull(metrics.netChange)
    }

    // --- average, most expensive day, savings ---

    @Test
    fun averageDailyExpense_divideByElapsedDaysOfTheCurrentWeek() {
        // Wednesday: 3 days elapsed.
        val metrics = compute(listOf(expense(90.0, "2026-11-09T10:00:00"), expense(30.0, "2026-11-11T10:00:00")))
        assertEquals(3, metrics.elapsedDays)
        assertEquals(40.0, metrics.averageDailyExpense, 0.0)
    }

    @Test
    fun averageDailyExpense_dividesBySevenForAPastWeek() {
        val metrics = compute(listOf(expense(70.0, "2026-11-03T10:00:00")), period = week.previous())
        assertEquals(7, metrics.elapsedDays)
        assertEquals(10.0, metrics.averageDailyExpense, 0.0)
    }

    @Test
    fun mostExpensiveDay_isTheOneWithTheHighestExpenses() {
        val metrics = compute(
            listOf(expense(10.0, "2026-11-09T10:00:00"), expense(60.0, "2026-11-10T10:00:00"), expense(25.0, "2026-11-11T10:00:00"))
        )
        assertEquals(LocalDate.parse("2026-11-10"), metrics.mostExpensiveDay?.date)
        assertEquals(60.0, metrics.mostExpensiveDay!!.expense, 0.0)
    }

    @Test
    fun mostExpensiveDay_tiesGoToTheEarliestDay() {
        val metrics = compute(listOf(expense(50.0, "2026-11-11T10:00:00"), expense(50.0, "2026-11-10T10:00:00")))
        assertEquals(LocalDate.parse("2026-11-10"), metrics.mostExpensiveDay?.date)
    }

    @Test
    fun mostExpensiveDay_isNullWhenNothingWasSpent() {
        assertNull(compute(listOf(income(10.0, "2026-11-10T10:00:00"))).mostExpensiveDay)
        assertNull(compute(emptyList()).mostExpensiveDay)
    }

    @Test
    fun savingsPercent_isIncomeLeftAfterExpenses() {
        assertEquals(75, MetricsAggregator.savingsPercent(income = 100.0, expense = 25.0))
        assertEquals(0, MetricsAggregator.savingsPercent(income = 100.0, expense = 100.0))
        assertEquals(-50, MetricsAggregator.savingsPercent(income = 100.0, expense = 150.0))
        assertEquals(100, MetricsAggregator.savingsPercent(income = 100.0, expense = 0.0))
    }

    @Test
    fun savingsPercent_isNullWithoutIncome() {
        assertNull(MetricsAggregator.savingsPercent(income = 0.0, expense = 50.0))
        assertNull(MetricsAggregator.savingsPercent(income = 0.0, expense = 0.0))
    }

    @Test
    fun savingsPercent_roundsToTheNearestWholePercent() {
        assertEquals(67, MetricsAggregator.savingsPercent(income = 300.0, expense = 100.0))
    }

    // --- categories ---

    @Test
    fun categories_keepOnlyPositiveOnesSortedByAmount() {
        val metrics = compute(
            emptyList(),
            categories = listOf(
                CategoryExpense("a", "A", "EXPENSE", 10.0),
                CategoryExpense("b", "B", "EXPENSE", 0.0),
                CategoryExpense("c", "C", "EXPENSE", 40.0)
            )
        )
        assertEquals(listOf("c", "a"), metrics.categories.map { it.typeId })
    }

    @Test
    fun categoryShares_areRelativeToTotalAndToTheTopCategory() {
        val shares = MetricsAggregator.categoryShares(
            listOf(
                CategoryExpense("c", "C", "EXPENSE", 60.0),
                CategoryExpense("a", "A", "EXPENSE", 30.0),
                CategoryExpense("d", "D", "EXPENSE", 10.0)
            )
        )
        assertEquals(listOf(60, 30, 10), shares.map { it.percent })
        assertEquals(1f, shares[0].barFraction, 0.0001f)
        assertEquals(0.5f, shares[1].barFraction, 0.0001f)
        assertEquals(1f / 6f, shares[2].barFraction, 0.0001f)
    }

    @Test
    fun categoryShares_emptyListGivesNothing() {
        assertTrue(MetricsAggregator.categoryShares(emptyList()).isEmpty())
    }

    // --- income sources ---

    @Test
    fun incomeSourceShares_sortBiggestFirstWithPercentAndBarFraction() {
        val shares = MetricsAggregator.incomeSourceShares(
            listOf(
                IncomeSourceTotal("b", "Becas", 100.0),
                IncomeSourceTotal("a", "Trabajo", 300.0)
            )
        )
        assertEquals(listOf("Trabajo", "Becas"), shares.map { it.name })
        assertEquals(listOf(75, 25), shares.map { it.percent })
        assertEquals(1f, shares[0].barFraction, 0f)
        assertEquals(1f / 3f, shares[1].barFraction, 0.0001f)
        assertEquals(listOf("a", "b"), shares.map { it.id })
    }

    @Test
    fun incomeSourceShares_groupIncomeWithoutASourceAsSinFuente() {
        val shares = MetricsAggregator.incomeSourceShares(
            listOf(
                IncomeSourceTotal("a", "Trabajo", 500.0),
                IncomeSourceTotal(null, null, 100.0),
                IncomeSourceTotal("gone", null, 100.0) // the source type no longer exists
            )
        )
        assertEquals(listOf("Trabajo", "Sin fuente"), shares.map { it.name })
        val none = shares.last()
        assertEquals(200.0, none.total, 0.0)
        assertNull(none.id)
        assertEquals(29, none.percent)
    }

    @Test
    fun incomeSourceShares_dropNonPositiveAndHandleEmpty() {
        assertTrue(MetricsAggregator.incomeSourceShares(emptyList()).isEmpty())
        assertTrue(MetricsAggregator.incomeSourceShares(listOf(IncomeSourceTotal("a", "X", 0.0))).isEmpty())
    }

    @Test
    fun compute_carriesIncomeSourcesAsShares() {
        val month = MetricsPeriod.containing(MetricsPeriodType.MONTH, today)
        val result = MetricsAggregator.compute(
            month, emptyList(), emptyList(), null, today, bogota,
            incomeSources = listOf(IncomeSourceTotal("a", "Trabajo", 10.0))
        )
        assertEquals(listOf("Trabajo"), result.incomeSources.map { it.name })
    }
}
