package dev.nonvocal.notes.api.controller;

import dev.nonvocal.notes.core.entity.Note;
import dev.nonvocal.notes.core.service.NoteService;

/**
 * Note controller (plain Java, no framework)
 */
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    public Note createNote(Note note) {
        return noteService.createNote(note);
    }

    public Note getNote(Long id) {
        return noteService.getNote(id);
    }

    public void deleteNote(Long id) {
        noteService.deleteNote(id);
    }
}
