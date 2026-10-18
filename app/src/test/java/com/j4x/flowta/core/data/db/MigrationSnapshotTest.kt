package com.j4x.flowta.core.data.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.j4x.flowta.database.FlowTaDatabase
import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM-side guard that mirrors SQLDelight's `verifyMigrations`: the committed snapshot
 * `src/main/sqldelight/databases/<version>.db` must match what `Schema.create` produces.
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

    @Test
    fun committedSnapshot_userVersionMatchesSchemaVersion() {
        val snapshot = File("src/main/sqldelight/databases/${FlowTaDatabase.Schema.version}.db")
        DriverManager.getConnection("jdbc:sqlite:${snapshot.path}").use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA user_version").use { rs ->
                    rs.next()
                    assertEquals(FlowTaDatabase.Schema.version, rs.getLong(1))
                }
            }
        }
    }
}
