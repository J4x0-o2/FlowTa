package com.j4x.flowta.feature.accounts.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.accounts.data.repository.AccountResetRepositoryImpl
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountResetDataTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: FlowTaDatabase
    private lateinit var repository: AccountResetRepositoryImpl
    private var nextId = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        db = FlowTaDatabase(driver)
        repository = newRepository(db.accountQueries)
        db.accountQueries.insert("a", "Cash", "CASH", 100.0, null, null, 1)
        db.accountQueries.insert("b", "Bank", "BANK", 200.0, null, null, 2)
        db.accountQueries.insert("c", "Wallet", "DIGITAL_WALLET", 300.0, null, null, 3)
        db.transactionTypeQueries.insert("type", "Salary", "INCOME", 1)
    }

    @After
    fun tearDown() = driver.close()

    private fun newRepository(accounts: AccountQueries) = AccountResetRepositoryImpl(
        accountQueries      = accounts,
        transactionQueries  = db.transactionQueries,
        transferQueries     = db.internalTransferQueries,
        goalQueries         = db.goalQueries,
        contributionQueries = db.goalContributionQueries
    )

    private fun tx(account: String) {
        db.transactionQueries.insert("t${nextId++}", 10.0, "INCOME", "type", null, account, null, 1_000)
    }

    private fun transfer(from: String, to: String) {
        db.internalTransferQueries.insert("x${nextId++}", from, to, null, 5.0, null, 1_000)
    }

    private fun balance(id: String) = db.accountQueries.selectById(id).executeAsOne().balance
    private fun txCount(id: String) = db.transactionQueries.countByAccount(id).executeAsOne()

    private fun seedMovements() {
        tx("a"); tx("a"); tx("b"); tx("c")
        transfer("a", "b"); transfer("b", "a"); transfer("b", "c")
    }

    @Test
    fun countMovements_countsTransactionsAndTransfersInEitherDirection() = runTest {
        seedMovements()

        assertEquals(4L, repository.countMovements("a").getOrThrow()) // 2 tx + 2 transfers
        assertEquals(2L, repository.countMovements("c").getOrThrow()) // 1 tx + 1 transfer (as destination)
        assertEquals(0L, repository.countMovements("unknown").getOrThrow())
    }

    @Test
    fun reset_removesOnlyTargetAccountMovements() = runTest {
        seedMovements()

        repository.reset("a", 0.0).getOrThrow()

        assertEquals(0L, txCount("a"))
        assertEquals(0L, db.internalTransferQueries.countByAccount("a").executeAsOne())
        assertEquals(1L, txCount("b"))
        assertEquals(1L, txCount("c"))
        // b -> c transfer survives; both a-related transfers are gone.
        assertEquals(1L, db.internalTransferQueries.countByAccount("b").executeAsOne())
    }

    @Test
    fun reset_setsBalanceToStartingValue_andLeavesOthersUntouched() = runTest {
        seedMovements()

        repository.reset("a", 5000.0).getOrThrow()

        assertEquals(5000.0, balance("a"), 0.0)
        assertEquals(200.0, balance("b"), 0.0)
        assertEquals(300.0, balance("c"), 0.0)
        assertEquals(3, db.accountQueries.selectAll().executeAsList().size)
    }

    @Test
    fun reset_withDefaultZero_setsZeroBalance() = runTest {
        repository.reset("a", 0.0).getOrThrow()
        assertEquals(0.0, balance("a"), 0.0)
    }

    @Test
    fun reset_removesGoalContributionsOfAccount_andTakesThemBackFromGoal() = runTest {
        db.goalQueries.insert("g1", "Trip", 1000.0, 300.0, "SAVINGS", "MONTHLY", null, 1)
        db.goalQueries.insert("g2", "Car", 5000.0, 50.0, "SAVINGS", "MONTHLY", null, 1)
        db.goalContributionQueries.insert("k1", "g1", "a", 200.0, null, 1)
        db.goalContributionQueries.insert("k2", "g1", "b", 100.0, null, 1)
        db.goalContributionQueries.insert("k3", "g2", "a", 80.0, null, 1) // exceeds current: floors at 0

        repository.reset("a", 0.0).getOrThrow()

        val goals = db.goalQueries.selectAll().executeAsList().associateBy { it.id }
        assertEquals(100.0, goals.getValue("g1").current_amount, 0.0)
        assertEquals(0.0, goals.getValue("g2").current_amount, 0.0)
        val remaining = db.goalContributionQueries.selectByGoal("g1").executeAsList()
        assertEquals(listOf("k2"), remaining.map { it.id })
        assertTrue(db.goalContributionQueries.selectByGoal("g2").executeAsList().isEmpty())
    }

    @Test
    fun reset_isAtomic_whenBalanceUpdateFails() = runTest {
        seedMovements()
        db.goalQueries.insert("g1", "Trip", 1000.0, 300.0, "SAVINGS", "MONTHLY", null, 1)
        db.goalContributionQueries.insert("k1", "g1", "a", 200.0, null, 1)
        val failing = spyk(db.accountQueries)
        every { failing.updateBalance(any(), any()) } throws IllegalStateException("boom")
        val failingRepository = newRepository(failing)

        val result = failingRepository.reset("a", 0.0)

        assertTrue(result.isFailure)
        assertEquals(2L, txCount("a"))
        assertEquals(4L, repository.countMovements("a").getOrThrow())
        assertEquals(100.0, balance("a"), 0.0)
        assertEquals(300.0, db.goalQueries.selectById("g1").executeAsOne().current_amount, 0.0)
        assertEquals(1, db.goalContributionQueries.selectByGoal("g1").executeAsList().size)
    }
}
