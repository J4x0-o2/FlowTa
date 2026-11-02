package com.j4x.flowta.core.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase
import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM-side guard that mirrors SQLDelight's `verifyMigrations`: the committed snapshot
 * `src/main/sqldelight/databases/<version>.db` must match what `Schema.create` produces.
 * The snapshot's version is its file name; the plugin leaves `PRAGMA user_version` at 0.
 *
 * Changing a `.sq` table without adding an `N.sqm` migration and regenerating the next
 * snapshot makes this test fail. Once migrations exist, extend it to open the previous
 * snapshot and run `FlowTaDatabase.Schema.migrate(driver, oldVersion, newVersion)`, then
 * compare against a fresh `Schema.create`.
 */
class MigrationSnapshotTest {

    private data class SchemaObject(val type: String, val name: String, val sql: String?)

    private fun readObjects(url: String): List<SchemaObject> =
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT type, name, sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type, name"
                ).use { rs ->
                    buildList { while (rs.next()) add(SchemaObject(rs.getString(1), rs.getString(2), rs.getString(3))) }
                }
            }
        }

    @Test
    fun committedSnapshot_matchesCurrentSchema() {
        val snapshot = File("src/main/sqldelight/databases/${FlowTaDatabase.Schema.version}.db")
        assertTrue("Missing schema snapshot ${snapshot.path}; run generate<Variant>FlowTaDatabaseSchema", snapshot.exists())

        val fresh = File.createTempFile("flowta-fresh", ".db").apply { deleteOnExit() }
        JdbcSqliteDriver("jdbc:sqlite:${fresh.path}").also {
            FlowTaDatabase.Schema.create(it)
            it.close()
        }

        val expected = readObjects("jdbc:sqlite:${fresh.path}")
        val actual = readObjects("jdbc:sqlite:${snapshot.path}")
        assertEquals(expected, actual)
    }

    // --- v1 -> v2 migration -------------------------------------------------------------

    private fun v1Driver(): JdbcSqliteDriver {
        val snapshot = File("src/main/sqldelight/databases/1.db")
        assertTrue("Missing frozen snapshot ${snapshot.path}", snapshot.exists())
        val copy = File.createTempFile("flowta-v1", ".db").apply { deleteOnExit() }
        snapshot.copyTo(copy, overwrite = true)
        return JdbcSqliteDriver("jdbc:sqlite:${copy.path}")
    }

    private fun JdbcSqliteDriver.exec(sql: String) = execute(null, sql, 0)

    private fun JdbcSqliteDriver.scalarLong(sql: String): Long =
        executeQuery(null, sql, { c -> app.cash.sqldelight.db.QueryResult.Value(if (c.next().value) c.getLong(0) else null) }, 0).value ?: error("no row")

    private fun JdbcSqliteDriver.migratedFromV1(): JdbcSqliteDriver {
        exec("INSERT INTO account(id, name, type, balance, created_at) VALUES('a1','Cash','CASH',10.0,1)")
        exec("INSERT INTO transaction_type(id, name, category, created_at) VALUES('t1','Salary','INCOME',1)")
        exec(
            "INSERT INTO user_transaction(id, amount, flow_type, transaction_type_id, account_id, created_at) " +
                "VALUES('old','5.0','INCOME','t1','a1',1)"
        )
        FlowTaDatabase.Schema.migrate(this, 1, 2)
        return this
    }

    private fun JdbcSqliteDriver.insertTx(id: String, source: String?, external: String?) {
        val s = source?.let { "'$it'" } ?: "NULL"
        val e = external?.let { "'$it'" } ?: "NULL"
        exec(
            "INSERT INTO user_transaction(id, amount, flow_type, transaction_type_id, account_id, created_at, source_app, external_id) " +
                "VALUES('$id',1.0,'INCOME','t1','a1',2,$s,$e)"
        )
    }

    @Test
    fun migrateV1ToV2_preservesExistingRowWithNullIntegrationColumns() {
        val driver = v1Driver().migratedFromV1()
        assertEquals(1L, driver.scalarLong("SELECT COUNT(*) FROM user_transaction WHERE id = 'old' AND source_app IS NULL AND external_id IS NULL"))
        driver.close()
    }

    @Test
    fun migrateV1ToV2_uniqueIndexRejectsDuplicateSourceAndExternalId() {
        val driver = v1Driver().migratedFromV1()
        driver.insertTx("n1", "micuadre", "42")
        try {
            driver.insertTx("n2", "micuadre", "42")
            fail("Expected the unique index to reject a duplicate (source_app, external_id)")
        } catch (expected: java.sql.SQLException) {
            // expected
        }
        driver.insertTx("n3", "micuadre", "43")
        driver.close()
    }

    @Test
    fun migrateV1ToV2_allowsManyRowsWithNullSource() {
        val driver = v1Driver().migratedFromV1()
        driver.insertTx("m1", null, null)
        driver.insertTx("m2", null, null)
        assertEquals(3L, driver.scalarLong("SELECT COUNT(*) FROM user_transaction"))
        driver.close()
    }

    @Test
    fun migrateV1ToV2_createsIntegrationLinkThatRoundTrips() {
        val driver = v1Driver().migratedFromV1()
        driver.exec("INSERT OR REPLACE INTO integration_link VALUES('micuadre','t1','a1','a1',7)")
        driver.exec("INSERT OR REPLACE INTO integration_link VALUES('micuadre','t2','a2','a3',8)")
        assertEquals(1L, driver.scalarLong("SELECT COUNT(*) FROM integration_link"))
        assertEquals(8L, driver.scalarLong("SELECT created_at FROM integration_link WHERE source_app = 'micuadre'"))
        driver.close()
    }

    @Test
    fun migratedV1Schema_matchesFreshCreate() {
        val migrated = v1Driver().also { FlowTaDatabase.Schema.migrate(it, 1, 2) }
        val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FlowTaDatabase.Schema.create(it) }
        val tables = listOf("user_transaction", "integration_link")
        for (t in tables) {
            assertEquals(t, columns(fresh, t), columns(migrated, t))
        }
        assertEquals(indexes(fresh), indexes(migrated))
        assertFalse(columns(migrated, "user_transaction").isEmpty())
        migrated.close()
        fresh.close()
    }

    private fun columns(d: JdbcSqliteDriver, table: String): List<String> =
        d.executeQuery(null, "SELECT name || ':' || type || ':' || \"notnull\" || ':' || pk FROM pragma_table_info('$table') ORDER BY cid", { c ->
            val out = mutableListOf<String>()
            while (c.next().value) out += c.getString(0)!!
            app.cash.sqldelight.db.QueryResult.Value(out)
        }, 0).value

    private fun indexes(d: JdbcSqliteDriver): List<String> =
        d.executeQuery(null, "SELECT name || ':' || tbl_name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%' ORDER BY name", { c ->
            val out = mutableListOf<String>()
            while (c.next().value) out += c.getString(0)!!
            app.cash.sqldelight.db.QueryResult.Value(out)
        }, 0).value
}
