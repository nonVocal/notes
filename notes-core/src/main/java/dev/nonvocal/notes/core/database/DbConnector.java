package dev.nonvocal.notes.core.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the H2 file-based database connection.
 * H2 stores the database as {@code notes.mv.db} inside {@code %APPDATA%\nvnotes\db\}.
 * The JDBC URL references the base name {@code notes} – H2 appends the {@code .mv.db} extension automatically.
 */
public class DbConnector {

    /**
     * Base path (without extension) of the H2 database file inside %APPDATA%.
     * H2 will create {@code notes.mv.db} at this location.
     */
    private static final String DB_RELATIVE_PATH =
            "nvnotes" + File.separator + "db" + File.separator + "notes";

    private final String jdbcUrl;

    /**
     * Creates a new {@code DbConnector} that resolves the database path
     * from the {@code APPDATA} environment variable.
     *
     * @throws IllegalStateException if the {@code APPDATA} environment variable is not set
     *                               or the database directory cannot be created
     */
    public DbConnector() {
        this(resolveDbFile());
    }

    /**
     * Creates a new {@code DbConnector} for the given database file.
     * This constructor is primarily intended for testing.
     *
     * @param dbFile the SQLite database file
     */
    public DbConnector(File dbFile) {
        ensureDirectoryExists(dbFile.getParentFile());
        // H2 file-mode URL; H2 appends ".mv.db" to the given path automatically
        this.jdbcUrl = "jdbc:h2:file:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=FALSE";
    }

    /**
     * Opens and returns a new {@link Connection} to the SQLite database.
     * The caller is responsible for closing the connection.
     *
     * @return a new JDBC connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static File resolveDbFile() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            throw new IllegalStateException(
                    "The APPDATA environment variable is not set. " +
                    "Cannot determine the database directory.");
        }
        return new File(appData, DB_RELATIVE_PATH);
    }

    private static void ensureDirectoryExists(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create database directory: " + directory.getAbsolutePath());
        }
    }
}

