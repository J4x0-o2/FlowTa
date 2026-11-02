package com.j4x.flowta.feature.integration.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.integration.data.repository.ExternalIncomeRepositoryImpl
import com.j4x.flowta.feature.integration.data.repository.IntegrationLinkRepositoryImpl
import com.j4x.flowta.feature.integration.domain.model.ExternalIncome
import com.j4x.flowta.feature.integration.domain.model.ExternalPaymentMethod
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.model.RecordExternalIncomeResult
import com.j4x.flowta.feature.integration.domain.usecase.RecordExternalIncomeUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordExternalIncomeTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: FlowTaDatabase
    private lateinit var links: IntegrationLinkRepositoryImpl
    private lateinit var useCase: RecordExternalIncomeUseCase

    private val link = IntegrationLink("com.micuadre", "trips", "cash", "bank", 5L)

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        db = FlowTaDatabase(driver)
        db.transactionTypeQueries.insert("trips", "Trips", "INCOME", 1)
        db.transactionTypeQueries.insert("food", "Food", "EXPENSE", 1)
        db.accountQueries.insert("cash", "Cash", "CASH", 100.0, null, null, 1)
        db.accountQueries.insert("bank", "Bank", "BANK", 200.0, null, null, 2)
        links = IntegrationLinkRepositoryImpl(db.integrationLinkQueries)
        useCase = RecordExternalIncomeUseCase(
            links,
            ExternalIncomeRepositoryImpl(db.transactionQueries, db.accountQueries, db.transactionTypeQueries)
        )
    }

    @After
    fun tearDown() = driver.close()

    private fun income(
        method: ExternalPaymentMethod = ExternalPaymentMethod.CASH,
        externalId: String = "trip-1",
        amountMinor: Long = 1_550_000L,
        occurredAt: Long = 1_700_000_000_000L,
        sourceApp: String = "com.micuadre"
    ) = ExternalIncome(sourceApp, externalId, method, amountMinor, occurredAt, "MiCuadre - Uber")

    private fun balance(id: String) = db.accountQueries.selectById(id).executeAsOne().balance
    private fun rowCount() = db.transactionQueries.selectAll().executeAsList().size

    @Test
    fun cash_routesToCashAccount_withTripTime() = runTest {
        links.save(link)

        val result = useCase(income(ExternalPaymentMethod.CASH))

        assertEquals(RecordExternalIncomeResult.Recorded, result)
        assertEquals(100.0 + 15_500.0, balance("cash"), 0.0001)
        assertEquals(200.0, balance("bank"), 0.0001)
        val row = db.transactionQueries.selectAll().executeAsList().single()
        assertEquals(1_700_000_000_000L, row.created_at)
        assertEquals("INCOME", row.flow_type)
        assertEquals("trips", row.transaction_type_id)
        assertEquals("cash", row.account_id)
        assertNull(row.source_type_id)
        assertEquals("MiCuadre - Uber", row.note)
        assertEquals(15_500.0, row.amount, 0.0001)
    }

    @Test
    fun transfer_routesToTransferAccount() = runTest {
        links.save(link)

        assertEquals(RecordExternalIncomeResult.Recorded, useCase(income(ExternalPaymentMethod.TRANSFER)))

        assertEquals(100.0, balance("cash"), 0.0001)
        assertEquals(200.0 + 15_500.0, balance("bank"), 0.0001)
    }

    @Test
    fun sameIds_secondCallIsDuplicate_withoutChangingBalance() = runTest {
        links.save(link)
        useCase(income())

        val second = useCase(income())

        assertEquals(RecordExternalIncomeResult.Duplicate, second)
        assertEquals(100.0 + 15_500.0, balance("cash"), 0.0001)
        assertEquals(1, rowCount())
    }

    @Test
    fun deletedMappedAccount_isNotLinked_andWritesNothing() = runTest {
        links.save(link)
        db.accountQueries.delete("bank")

        val result = useCase(income(ExternalPaymentMethod.TRANSFER))

        assertEquals(RecordExternalIncomeResult.NotLinked, result)
        assertEquals(0, rowCount())
    }

    @Test
    fun deletedIncomeType_isNotLinked() = runTest {
        links.save(link)
        db.transactionTypeQueries.delete("trips")

        assertEquals(RecordExternalIncomeResult.NotLinked, useCase(income()))
        assertEquals(0, rowCount())
        assertEquals(100.0, balance("cash"), 0.0001)
    }

    @Test
    fun nonIncomeType_isNotLinked() = runTest {
        links.save(link.copy(incomeTypeId = "food"))

        assertEquals(RecordExternalIncomeResult.NotLinked, useCase(income()))
        assertEquals(0, rowCount())
    }

    @Test
    fun noLink_isNotLinked() = runTest {
        assertEquals(RecordExternalIncomeResult.NotLinked, useCase(income()))
        assertEquals(0, rowCount())
    }

    @Test
    fun invalidInput_isRejected_andWritesNothing() = runTest {
        links.save(link)
        val invalid = listOf(
            income(sourceApp = " "),
            income(externalId = ""),
            income(amountMinor = 0),
            income(amountMinor = -5),
            income(occurredAt = 0)
        )

        invalid.forEach { assertTrue(useCase(it) is RecordExternalIncomeResult.Invalid) }

        assertEquals(0, rowCount())
        assertEquals(100.0, balance("cash"), 0.0001)
    }

    @Test
    fun invalidInput_winsOverMissingLink() = runTest {
        assertTrue(useCase(income(amountMinor = 0)) is RecordExternalIncomeResult.Invalid)
    }

    @Test
    fun manualTransactions_withNullSource_areUnaffected() = runTest {
        links.save(link)
        db.transactionQueries.insert("m1", 5.0, "INCOME", "trips", null, "cash", null, 1)
        db.transactionQueries.insert("m2", 5.0, "INCOME", "trips", null, "cash", null, 2)

        assertEquals(RecordExternalIncomeResult.Recorded, useCase(income()))

        assertEquals(3, rowCount())
    }

    @Test
    fun linkRepository_saveGetObserveDelete_roundTrip() = runTest {
        assertNull(links.get("com.micuadre"))

        links.save(link)
        assertEquals(link, links.get("com.micuadre"))
        assertEquals(link, links.observe("com.micuadre").first())

        links.save(link.copy(cashAccountId = "bank"))
        assertEquals("bank", links.get("com.micuadre")!!.cashAccountId)

        links.delete("com.micuadre")
        assertNull(links.get("com.micuadre"))
        assertNull(links.observe("com.micuadre").first())
    }
}
