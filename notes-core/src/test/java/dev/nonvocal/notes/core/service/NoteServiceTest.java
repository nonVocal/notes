package dev.nonvocal.notes.core.service;

import dev.nonvocal.notes.core.database.DbConnector;
import dev.nonvocal.notes.core.database.NoteRepository;
import dev.nonvocal.notes.core.entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NoteService.
 *
 * <p>Each test run gets its own temporary H2 database so that the
 * production database ({@code %APPDATA%\nvnotes\db\notes}) is never touched.
 */
class NoteServiceTest {

    @TempDir
    Path tempDir;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        // Use a fresh, isolated H2 database in the JUnit temp directory
        File tempDb = tempDir.resolve("notes-test").toFile();
        DbConnector testConnector = new DbConnector(tempDb);
        noteService = new NoteServiceImpl(new NoteRepository(testConnector));
    }

    @Test
    void testCreateNote() {
        Note note = new Note("Test", "Test Content");
        Note result = noteService.createNote(note);
        assertNotNull(result);
        assertEquals("Test", result.getTitle());
    }
}
