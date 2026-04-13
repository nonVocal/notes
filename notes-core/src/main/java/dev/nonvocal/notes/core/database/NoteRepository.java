package dev.nonvocal.notes.core.database;

import dev.nonvocal.notes.core.entity.Note;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles all read and write operations for {@link Note} objects in the H2 database.
 * The table {@code notes} is created automatically if it does not yet exist.
 *
 * <p>Example usage:
 * <pre>{@code
 * NoteRepository repo = new NoteRepository(new DbConnector());
 * Note saved = repo.save(new Note("Title", "Content"));
 * Optional<Note> found = repo.findById(saved.getId());
 * repo.delete(saved.getId());
 * }</pre>
 */
public class NoteRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS notes (
                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                title       VARCHAR(255) NOT NULL,
                content     CLOB,
                created_at  TIMESTAMP NOT NULL,
                modified_at TIMESTAMP NOT NULL
            )
            """;

    // Migration: add columns to existing databases that were created without them
    private static final String ADD_CREATED_AT_SQL =
            "ALTER TABLE notes ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP";
    private static final String ADD_MODIFIED_AT_SQL =
            "ALTER TABLE notes ADD COLUMN IF NOT EXISTS modified_at TIMESTAMP";

    private static final String INSERT_SQL =
            "INSERT INTO notes (title, content, created_at, modified_at) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE notes SET title = ?, content = ?, modified_at = ? WHERE id = ?";

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, title, content, created_at, modified_at FROM notes WHERE id = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, title, content, created_at, modified_at FROM notes ORDER BY id";

    private static final String DELETE_SQL =
            "DELETE FROM notes WHERE id = ?";

    private final DbConnector dbConnector;

    /**
     * Creates a new {@code NoteRepository} and initialises the database schema.
     *
     * @param dbConnector the connector used to obtain database connections
     * @throws IllegalStateException if the schema cannot be initialised
     */
    public NoteRepository(DbConnector dbConnector) {
        this.dbConnector = dbConnector;
        initSchema();
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Persists a {@link Note}.
     * <ul>
     *   <li>If {@code note.getId()} is {@code null} an INSERT is performed and
     *       the generated id is set on the returned note.</li>
     *   <li>Otherwise an UPDATE is performed.</li>
     * </ul>
     *
     * @param note the note to persist (must not be {@code null})
     * @return the persisted note with its id populated
     * @throws SQLException if a database error occurs
     */
    public Note save(Note note) throws SQLException {
        if (note.getId() == null) {
            return insert(note);
        } else {
            return update(note);
        }
    }

    /**
     * Deletes the note with the given id. Does nothing if no such note exists.
     *
     * @param id the id of the note to delete
     * @throws SQLException if a database error occurs
     */
    public void delete(Long id) throws SQLException {
        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Finds a note by its id.
     *
     * @param id the id to look up
     * @return an {@link Optional} containing the note, or empty if not found
     * @throws SQLException if a database error occurs
     */
    public Optional<Note> findById(Long id) throws SQLException {
        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all notes ordered by id ascending.
     *
     * @return a list of all notes; empty if none exist
     * @throws SQLException if a database error occurs
     */
    public List<Note> findAll() throws SQLException {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                notes.add(mapRow(rs));
            }
        }
        return notes;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void initSchema() {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            // Ensure timestamp columns exist for databases created before this migration
            stmt.execute(ADD_CREATED_AT_SQL);
            stmt.execute(ADD_MODIFIED_AT_SQL);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialise the notes table", e);
        }
    }

    private Note insert(Note note) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getContent());
            stmt.setTimestamp(3, Timestamp.valueOf(now));
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    note.setId(keys.getLong(1));
                }
            }
        }
        note.setCreatedAt(now);
        note.setModifiedAt(now);
        return note;
    }

    private Note update(Note note) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getContent());
            stmt.setTimestamp(3, Timestamp.valueOf(now));
            stmt.setLong(4, note.getId());
            stmt.executeUpdate();
        }
        note.setModifiedAt(now);
        return note;
    }

    private static Note mapRow(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getLong("id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            note.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp modifiedAt = rs.getTimestamp("modified_at");
        if (modifiedAt != null) {
            note.setModifiedAt(modifiedAt.toLocalDateTime());
        }
        return note;
    }
}

