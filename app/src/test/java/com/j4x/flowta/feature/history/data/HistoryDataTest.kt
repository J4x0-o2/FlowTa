package com.j4x.flowta.feature.history.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.history.data.repository.HistoryRepositoryImpl
import com.j4x.flowta.feature.history.domain.model.HistoryMovement
import com.j4x.flowta.feature.history.domain.model.PeriodTotals
import com.j4x.flowta.feature.history.domain.usecase.ObserveMonthHistoryUseCase
import com.j4x.flowta.feature.history.domain.usecase.ObserveYearHistoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class HistoryDataTest {

    private val bogota = ZoneId.of("America/Bogota")
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: FlowTaDatabase
    private lateinit var repository: HistoryRepositoryImpl
    private var nextId = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        db = FlowTaDatabase(driver)
        repository = HistoryRepositoryImpl(db.transactionQueries)
    }

    @After
    fun tearDown() = driver.close()

    private fun millis(local: String): Long =
        LocalDateTime.parse(local).atZone(bogota).toInstant().toEpochMilli()

    private fun insert(flow: String, amount: Double, createdAt: Long) {
        db.transactionQueries.insert("t${nextId++}", amount, flow, "type", null, "account", null, createdAt)
    }

    @Test
    fun observeMovements_isHalfOpenRange_oldestFirst() = runTest {
        insert("EXPENSE", 2.0, 2_000)
        insert("INCOME", 1.0, 1_000)
        insert("INCOME", 9.0, 3_000) // upper bound is exclusive
        insert("INCOME", 8.0, 999)

        val result = repository.observeMovements(1_000, 3_000).first()

        assertEquals(
            listOf(HistoryMovement("INCOME", 1.0, 1_000), HistoryMovement("EXPENSE", 2.0, 2_000)),
            result
        )
    }

    @Test
    fun observeMovements_ignoresTransfers() = runTest {
        insert("INCOME", 1.0, 1_000)
        db.internalTransferQueries.insert("x1", "a", "b", null, 500.0, null, 1_000)

        assertEquals(1, repository.observeMovements(0, 10_000).first().size)
    }

    @Test
    fun observeMovements_reEmitsWhenAMovementIsInserted() = runTest {
        repository.observeMovements(0, 10_000).test {
            assertEquals(0, awaitItem().size)
            insert("INCOME", 1.0, 5)
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeEarliest_isNullWhenEmpty_thenMinCreatedAt() = runTest {
        assertNull(repository.observeEarliest().first())
        insert("INCOME", 1.0, 700)
        insert("EXPENSE", 1.0, 300)
        assertEquals(300L, repository.observeEarliest().first())
    }

    @Test
    fun yearUseCase_groupsByLocalMonth_notUtc() = runTest {
        // 2026-10-31 23:30 in Bogota is already November 1st in UTC.
        insert("INCOME", 50.0, millis("2026-10-31T23:30:00"))
        insert("EXPENSE", 20.0, millis("2026-03-02T10:00:00"))
        insert("INCOME", 5.0, millis("2025-12-31T23:30:00"))

        val result = ObserveYearHistoryUseCase(repository)(2026, bogota, LocalDate.of(2026, 11, 3)).first()

        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 3)), result.months.map { it.month })
        assertEquals(PeriodTotals(50.0, 20.0, 2), result.totals)
        assertEquals(true, result.hasPreviousYear)
        assertEquals(true, result.isCurrentYear)
    }

    @Test
    fun monthUseCase_returnsWeeksOfThatMonthOnly() = runTest {
        insert("INCOME", 10.0, millis("2026-10-04T23:30:00"))
        insert("EXPENSE", 3.0, millis("2026-10-05T00:10:00"))
        insert("INCOME", 99.0, millis("2026-11-01T00:10:00"))

        val result = ObserveMonthHistoryUseCase(repository)(2026, 10, bogota, LocalDate.of(2026, 11, 20)).first()

        assertEquals(5, result.weeks.size)
        assertEquals(PeriodTotals(10.0, 0.0, 1), result.weeks[0].totals)
        assertEquals(PeriodTotals(0.0, 3.0, 1), result.weeks[1].totals)
        assertEquals(PeriodTotals(10.0, 3.0, 2), result.totals)
    }
}
