package com.j4x.flowta.feature.integration.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.integration.data.repository.IntegrationLinkRepositoryImpl
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IntegrationLinkRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: IntegrationLinkRepositoryImpl

    private val a = IntegrationLink("com.a", "t1", "c1", "c2", 1L)
    private val b = IntegrationLink("com.b", "t2", "c3", "c4", 2L)

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        repo = IntegrationLinkRepositoryImpl(FlowTaDatabase(driver).integrationLinkQueries)
    }

    @After
    fun tearDown() = driver.close()

    @Test
    fun observeAll_emitsEmptyThenFollowsSaveAndDelete() = runTest {
        repo.observeAll().test {
            assertEquals(emptyList<IntegrationLink>(), awaitItem())

            repo.save(a)
            assertEquals(listOf(a), awaitItem())

            repo.save(b)
            assertEquals(setOf(a, b), awaitItem().toSet())

            repo.delete("com.a")
            assertEquals(listOf(b), awaitItem())
        }
    }

    @Test
    fun observeAll_reflectsUpsertOfSameSource() = runTest {
        repo.save(a)
        repo.observeAll().test {
            assertEquals(listOf(a), awaitItem())

            val updated = a.copy(cashAccountId = "c9")
            repo.save(updated)
            assertEquals(listOf(updated), awaitItem())
        }
    }
}
