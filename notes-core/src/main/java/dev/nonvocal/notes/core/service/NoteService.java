package dev.nonvocal.notes.core.service;

import dev.nonvocal.notes.core.entity.Note;

/**
 * Note service interface
 */
public interface NoteService {
    Note createNote(Note note);
    Note getNote(Long id);
    void deleteNote(Long id);
}

