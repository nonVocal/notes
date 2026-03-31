package dev.nonvocal.notes.core.service;

import dev.nonvocal.notes.core.entity.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NoteService
 */
class NoteServiceTest {

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteServiceImpl();
    }

    @Test
    void testCreateNote() {
        Note note = new Note("Test", "Test Content");
        Note result = noteService.createNote(note);
        assertNotNull(result);
        assertEquals("Test", result.getTitle());
    }
}
