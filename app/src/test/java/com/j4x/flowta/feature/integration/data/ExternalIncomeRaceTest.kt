package com.j4x.flowta.feature.integration.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.integration.data.repository.ExternalIncomeRepositoryImpl
import com.j4x.flowta.feature.integration.domain.model.ExternalIncome
import com.j4x.flowta.feature.integration.domain.model.ExternalPaymentMethod
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.model.RecordExternalIncomeResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.sql.SQLException

/**
 * Simulates losing the race on the unique index: the pre-check sees nothing, the insert then fails
 * because a concurrent call committed the same (source_app, external_id) first.
 */
class ExternalIncomeRaceTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: FlowTaDatabase

    private val link = IntegrationLink("com.micuadre", "trips", "cash", "bank", 5L)
    private val income = ExternalIncome(
        "com.micuadre", "trip-1", ExternalPaymentMethod.CASH, 1_000_00L, 1_700_000_000_000L, null
    )

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        db = FlowTaDatabase(driver)
        db.transactionTypeQueries.insert("trips", "Trips", "INCOME", 1)
        db.accountQueries.insert("cash", "Cash", "CASH", 100.0, null, null, 1)
        db.accountQueries.insert("bank", "Bank", "BANK", 200.0, null, null, 2)
    }

    @After
    fun tearDown() = driver.close()

    /** Pre-check (inside the transaction) reports [first]; any later check reports [later]. */
    private fun repoWhoseInsertFails(first: Long, later: Long, failure: Throwable): ExternalIncomeRepositoryImpl {
        val count = mockk<Query<Long>>()
        every { count.executeAsOne() } returnsMany listOf(first, later)
        val queries = spyk(db.transactionQueries)
        every { queries.countBySourceExternal(any(), any()) } returns count
        every { queries.insertExternal(any(), any(), any(), any(), any(), any(), any(), any()) } throws failure
        return ExternalIncomeRepositoryImpl(queries, db.accountQueries, db.transactionTypeQueries)
    }

    @Test
    fun lostRace_onUniqueIndex_returnsDuplicate_andLeavesBalanceUntouched() = runTest {
        val repo = repoWhoseInsertFails(0L, 1L, SQLException("UNIQUE constraint failed"))

        assertEquals(RecordExternalIncomeResult.Duplicate, repo.record(income, link))
        assertEquals(100.0, db.accountQueries.selectById("cash").executeAsOne().balance, 0.0001)
    }

    @Test
    fun unrelatedFailure_isRethrown_whenNoRowExists() = runTest {
        val repo = repoWhoseInsertFails(0L, 0L, SQLException("disk I/O error"))

        assertThrows(SQLException::class.java) { kotlinx.coroutines.runBlocking { repo.record(income, link) } }
    }
}
