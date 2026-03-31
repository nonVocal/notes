package dev.nonvocal.notes.web;

import dev.nonvocal.notes.web.servlet.NotesApiServlet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for the Notes web module.
 */
class NotesWebTest {

    @Test
    void notesApiServlet_canBeInstantiated() {
        assertDoesNotThrow(NotesApiServlet::new);
    }
}
