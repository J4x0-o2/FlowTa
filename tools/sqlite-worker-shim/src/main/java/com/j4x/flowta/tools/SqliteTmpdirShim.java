package com.j4x.flowta.tools;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Gradle 9 starts process-isolated workers with an almost empty environment (only SystemRoot),
 * so on Windows java.io.tmpdir resolves to C:\Windows and sqlite-jdbc cannot extract its native
 * library there. DriverManager loads every java.sql.Driver service before SQLDelight opens its
 * connection, so this no-op driver points sqlite-jdbc at a writable directory first.
 */
public final class SqliteTmpdirShim implements Driver {
    static {
        if (System.getProperty("org.sqlite.tmpdir") == null) {
            File dir = new File(System.getProperty("user.home"), ".gradle/sqlite-native");
            dir.mkdirs();
            System.setProperty("org.sqlite.tmpdir", dir.getAbsolutePath());
        }
    }

    @Override public Connection connect(String url, Properties info) { return null; }
    @Override public boolean acceptsURL(String url) { return false; }
    @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
    @Override public int getMajorVersion() { return 1; }
    @Override public int getMinorVersion() { return 0; }
    @Override public boolean jdbcCompliant() { return false; }
    @Override public Logger getParentLogger() { return Logger.getGlobal(); }
}
