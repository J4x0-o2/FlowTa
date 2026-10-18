package com.j4x.flowta.core.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the database schema contract.
 *
 * Schema v1 is the baseline shipped to devices (AndroidSqliteDriver stores
 * `user_version = Schema.version`). When the schema changes, add `N.sqm` migrations,
 * regenerate the `N+1.db` snapshot and extend this test to open a driver on the previous
 * snapshot and run `FlowTaDatabase.Schema.migrate(...)`.
 */
class DatabaseSchemaTest {

    private fun newDriver() = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        FlowTaDatabase.Schema.create(it)
    }

    @Test
    fun schemaVersion_isOne_untilFirstMigrationIsAdded() {
        assertEquals(1L, FlowTaDatabase.Schema.version)
    }

    @Test
    fun create_producesExpectedTables() {
        val driver = newDriver()
        val tables = mutableSetOf<String>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'",
            mapper = { cursor ->
                while (cursor.next().value) tables += cursor.getString(0)!!
                app.cash.sqldelight.db.QueryResult.Unit
            },
            parameters = 0
        )
        assertEquals(
            setOf(
                "account", "goal", "goal_contribution",
                "internal_transfer", "transaction_type", "user_transaction"
            ),
            tables
        )
        driver.close()
    }
}
