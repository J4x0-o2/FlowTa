package com.j4x.flowta.feature.types.domain

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.types.data.repository.TransactionTypeRepositoryImpl
import com.j4x.flowta.feature.types.domain.usecase.SeedDefaultTypesUseCase
import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeedDefaultTypesTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: FlowTaDatabase

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        db = FlowTaDatabase(driver)
    }

    @After
    fun tearDown() {
        unmockkAll()
        driver.close()
    }

    private fun typeCount() = db.transactionTypeQueries.selectAll().executeAsList().size

    @Test
    fun seed_insertsEveryDefaultType() = runTest {
        val result = SeedDefaultTypesUseCase(TransactionTypeRepositoryImpl(db.transactionTypeQueries))()

        assertTrue(result.isSuccess)
        assertEquals(23, typeCount())
    }

    @Test
    fun seed_isANoOp_whenTypesAlreadyExist() = runTest {
        db.transactionTypeQueries.insert("x", "Mine", "EXPENSE", 1)

        val result = SeedDefaultTypesUseCase(TransactionTypeRepositoryImpl(db.transactionTypeQueries))()

        assertTrue(result.isSuccess)
        assertEquals(1, typeCount())
    }

    @Test
    fun seed_whenAnInsertFails_leavesZeroTypes_returnsFailure_andDoesNotThrow() = runTest {
        val failing = spyk(db.transactionTypeQueries)
        var calls = 0
        every { failing.insert(any(), any(), any(), any()) } answers {
            if (++calls == 5) throw IllegalStateException("boom")
            callOriginal()
        }

        val result = SeedDefaultTypesUseCase(TransactionTypeRepositoryImpl(failing))()

        assertTrue(result.isFailure)
        assertEquals(0, typeCount())

        // A later seed (fresh queries, no failure) succeeds because nothing was left half-seeded.
        val retry = SeedDefaultTypesUseCase(TransactionTypeRepositoryImpl(db.transactionTypeQueries))()
        assertTrue(retry.isSuccess)
        assertEquals(23, typeCount())
    }

    @Test
    fun seed_whenCallersOverlapOnAnEmptyDatabase_insertsTheDefaultsExactlyOnce() {
        repeat(30) { round ->
            db.transactionTypeQueries.deleteAll()
            val results = runBlocking {
                (1..8).map {
                    async(Dispatchers.Default) {
                        SeedDefaultTypesUseCase(TransactionTypeRepositoryImpl(db.transactionTypeQueries))()
                    }
                }.awaitAll()
            }

            assertTrue("round $round", results.all { it.isSuccess })
            assertEquals("round $round", 23, typeCount())
        }
    }
}
