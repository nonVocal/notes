package dev.nonvocal.notes.osgi.impl;

import dev.nonvocal.notes.core.entity.Note;
import dev.nonvocal.notes.core.service.NoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NoteComponentImpl}.
 * The OSGi runtime is not required – we wire dependencies manually.
 */
class NoteComponentImplTest {

    private NoteComponentImpl component;

    @BeforeEach
    void setUp() {
        component = new NoteComponentImpl();
        component.setNoteService(new NoteServiceImpl());
        component.activate();
    }

    @Test
    void createNote_returnsNote() {
        Note note = new Note("Test", "Content");
        Note result = component.create(note);
        assertNotNull(result);
        assertEquals("Test", result.getTitle());
    }

    @Test
    void deleteNote_doesNotThrow() {
        assertDoesNotThrow(() -> component.delete(999L));
    }
}

