package com.j4x.flowta.feature.metrics.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase
import com.j4x.flowta.feature.metrics.data.repository.MetricsRepositoryImpl
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import com.j4x.flowta.feature.metrics.domain.model.IncomeSourceTotal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MetricsDataTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: FlowTaDatabase
    private lateinit var repository: MetricsRepositoryImpl
    private var nextId = 0

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        db = FlowTaDatabase(driver)
        repository = MetricsRepositoryImpl(db.transactionQueries)
        db.transactionTypeQueries.insert("job", "Trabajo", "INCOME_SOURCE", 0)
        db.transactionTypeQueries.insert("gift", "Regalos", "INCOME_SOURCE", 0)
        db.transactionTypeQueries.insert("food", "Comida", "EXPENSE", 0)
    }

    @After
    fun tearDown() = driver.close()

    private fun insert(flow: String, amount: Double, createdAt: Long, source: String? = null) {
        val type = if (flow == "INCOME") "salary" else "food"
        db.transactionQueries.insert("t${nextId++}", amount, flow, type, source, "account", null, createdAt)
    }

    @Test
    fun incomeBySource_sumsPerSourceBiggestFirst() = runTest {
        insert("INCOME", 100.0, 1_000, "job")
        insert("INCOME", 300.0, 1_500, "job")
        insert("INCOME", 50.0, 1_200, "gift")

        val result = repository.getIncomeBySource(DateRange(1_000, 2_000)).first()

        assertEquals(
            listOf(IncomeSourceTotal("job", "Trabajo", 400.0), IncomeSourceTotal("gift", "Regalos", 50.0)),
            result
        )
    }

    @Test
    fun incomeBySource_groupsIncomeWithoutSourceUnderANullId() = runTest {
        insert("INCOME", 70.0, 1_000, null)
        insert("INCOME", 30.0, 1_100, null)

        val result = repository.getIncomeBySource(DateRange(0, 2_000)).first()

        assertEquals(listOf(IncomeSourceTotal(null, null, 100.0)), result)
    }

    @Test
    fun incomeBySource_isHalfOpenAndIgnoresExpenses() = runTest {
        insert("INCOME", 10.0, 1_000, "job") // lower bound is inclusive
        insert("INCOME", 99.0, 2_000, "job") // upper bound is exclusive
        insert("INCOME", 98.0, 999, "job")
        insert("EXPENSE", 500.0, 1_500, null)

        val result = repository.getIncomeBySource(DateRange(1_000, 2_000)).first()

        assertEquals(listOf(IncomeSourceTotal("job", "Trabajo", 10.0)), result)
    }

    @Test
    fun incomeBySource_isEmptyWithoutIncome() = runTest {
        assertEquals(emptyList<IncomeSourceTotal>(), repository.getIncomeBySource(DateRange(0, 10)).first())
    }
}
