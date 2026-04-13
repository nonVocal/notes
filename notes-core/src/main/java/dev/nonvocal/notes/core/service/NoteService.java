package dev.nonvocal.notes.core.service;

import dev.nonvocal.notes.core.entity.Note;

import java.util.List;

/**
 * Note service interface
 */
public interface NoteService {
    Note createNote(Note note);
    Note getNote(Long id);
    List<Note> getAllNotes();
    void deleteNote(Long id);
}

