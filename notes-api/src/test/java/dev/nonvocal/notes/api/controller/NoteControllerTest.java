package dev.nonvocal.notes.api.controller;

import dev.nonvocal.notes.core.entity.Note;
import dev.nonvocal.notes.core.service.NoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NoteController
 */
class NoteControllerTest {

    private NoteController controller;

    @BeforeEach
    void setUp() {
        controller = new NoteController(new NoteServiceImpl());
    }

    @Test
    void testCreateNote() {
        Note note = new Note("Test", "Content");
        Note result = controller.createNote(note);
        assertNotNull(result);
        assertEquals("Test", result.getTitle());
    }
}
