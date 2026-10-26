package com.j4x.flowta.feature.settings.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.core.domain.work.WorkCanceller
import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.settings.data.repository.AppDataRepositoryImpl
import com.j4x.flowta.feature.settings.domain.model.UserPreferences
import com.j4x.flowta.feature.settings.domain.repository.SettingsRepository
import com.j4x.flowta.feature.settings.domain.usecase.ResetAllDataUseCase
import com.j4x.flowta.feature.types.data.repository.TransactionTypeRepositoryImpl
import com.j4x.flowta.feature.types.domain.usecase.SeedDefaultTypesUseCase
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResetAllDataTest {

    private class FakeSettings(private val events: MutableList<String>) : SettingsRepository {
        var clearCalls = 0
        override fun getPreferences(): Flow<UserPreferences> = emptyFlow()
        override suspend fun setTheme(theme: String) = Unit
        override suspend fun setCurrency(code: String) = Unit
        override suspend fun setLanguage(language: String) = Unit
        override suspend fun clear() { clearCalls++; events += "settings.clear" }
    }

    private class FakeWork(private val events: MutableList<String>) : WorkCanceller {
        var cancelCalls = 0
        override fun cancelAll() { cancelCalls++; events += "work.cancel" }
    }

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: FlowTaDatabase
    private val events = mutableListOf<String>()
    private lateinit var settings: FakeSettings
    private lateinit var work: FakeWork

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        db = FlowTaDatabase(driver)
        settings = FakeSettings(events)
        work = FakeWork(events)
        db.transactionTypeQueries.insert("custom", "Mi categoría", "EXPENSE", 1)
        db.accountQueries.insert("a", "Cash", "CASH", 100.0, null, null, 1)
        db.accountQueries.insert("b", "Bank", "BANK", 200.0, null, null, 2)
        db.transactionQueries.insert("t1", 10.0, "EXPENSE", "custom", null, "a", null, 1)
        db.internalTransferQueries.insert("x1", "a", "b", null, 5.0, null, 1)
        db.goalQueries.insert("g1", "Trip", 1000.0, 200.0, "SAVINGS", "MONTHLY", null, 1)
        db.goalContributionQueries.insert("k1", "g1", "a", 200.0, null, 1)
    }

    @After
    fun tearDown() = driver.close()

    private fun appData(accounts: AccountQueries = db.accountQueries) = AppDataRepositoryImpl(
        accountQueries      = accounts,
        transactionQueries  = db.transactionQueries,
        transferQueries     = db.internalTransferQueries,
        typeQueries         = db.transactionTypeQueries,
        goalQueries         = db.goalQueries,
        contributionQueries = db.goalContributionQueries
    )

    private fun useCase(appData: AppDataRepositoryImpl = appData()) = ResetAllDataUseCase(
        appData          = appData,
        settings         = settings,
        workCanceller    = work,
        seedDefaultTypes = SeedDefaultTypesUseCase(TransactionTypeRepositoryImpl(db.transactionTypeQueries))
    )

    @Test
    fun wipeAll_emptiesEveryTable() = runTest {
        appData().wipeAll().getOrThrow()

        assertTrue(db.accountQueries.selectAll().executeAsList().isEmpty())
        assertTrue(db.transactionQueries.selectAll().executeAsList().isEmpty())
        assertTrue(db.internalTransferQueries.selectAll().executeAsList().isEmpty())
        assertTrue(db.goalQueries.selectAll().executeAsList().isEmpty())
        assertTrue(db.goalContributionQueries.selectByGoal("g1").executeAsList().isEmpty())
        assertTrue(db.transactionTypeQueries.selectAll().executeAsList().isEmpty())
    }

    @Test
    fun wipeAll_isAtomic_whenALaterDeleteFails() = runTest {
        val failing = spyk(db.accountQueries)
        every { failing.deleteAll() } throws IllegalStateException("boom") // runs last

        val result = appData(failing).wipeAll()

        assertTrue(result.isFailure)
        assertEquals(2, db.accountQueries.selectAll().executeAsList().size)
        assertEquals(1, db.transactionQueries.selectAll().executeAsList().size)
        assertEquals(1, db.internalTransferQueries.selectAll().executeAsList().size)
        assertEquals(1, db.goalQueries.selectAll().executeAsList().size)
        assertEquals(1, db.goalContributionQueries.selectByGoal("g1").executeAsList().size)
        assertEquals(1, db.transactionTypeQueries.selectAll().executeAsList().size)
    }

    @Test
    fun resetAll_leavesOnlyReseededDefaultTypes() = runTest {
        useCase().invoke().getOrThrow()

        assertTrue(db.accountQueries.selectAll().executeAsList().isEmpty())
        assertTrue(db.transactionQueries.selectAll().executeAsList().isEmpty())
        assertTrue(db.internalTransferQueries.selectAll().executeAsList().isEmpty())
        assertTrue(db.goalQueries.selectAll().executeAsList().isEmpty())

        val types = db.transactionTypeQueries.selectAll().executeAsList()
        assertEquals(23, types.size)
        assertEquals(
            mapOf("INCOME" to 5, "EXPENSE" to 9, "TRANSFER" to 2, "INCOME_SOURCE" to 7),
            types.groupingBy { it.category }.eachCount()
        )
        assertTrue(types.none { it.id == "custom" })
    }

    @Test
    fun resetAll_clearsPreferences_andCancelsWork_afterTheWipe() = runTest {
        useCase().invoke().getOrThrow()

        assertEquals(1, settings.clearCalls)
        assertEquals(1, work.cancelCalls)
        assertEquals(setOf("settings.clear", "work.cancel"), events.toSet())
    }

    @Test
    fun resetAll_whenWipeFails_returnsFailure_andDoesNotTouchPreferencesOrWork() = runTest {
        val failing = spyk(db.accountQueries)
        every { failing.deleteAll() } throws IllegalStateException("boom")

        val result = useCase(appData(failing)).invoke()

        assertTrue(result.isFailure)
        assertEquals(0, settings.clearCalls)
        assertEquals(0, work.cancelCalls)
        assertEquals(1, db.transactionTypeQueries.selectAll().executeAsList().size) // not re-seeded
    }
}
