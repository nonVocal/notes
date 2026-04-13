package dev.nonvocal.notes.core.service;

import dev.nonvocal.notes.core.database.DbConnector;
import dev.nonvocal.notes.core.database.NoteRepository;
import dev.nonvocal.notes.core.entity.Note;

import java.sql.SQLException;

/**
 * Note service implementation backed by a {@link NoteRepository}.
 * Creates and manages its own {@link DbConnector} and {@link NoteRepository}.
 */
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;

    public NoteServiceImpl() {
        this.noteRepository = new NoteRepository(new DbConnector());
    }

    @Override
    public Note createNote(Note note) {
        try {
            return noteRepository.save(note);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create note", e);
        }
    }

    @Override
    public Note getNote(Long id) {
        try {
            return noteRepository.findById(id).orElse(null);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve note with id " + id, e);
        }
    }

    @Override
    public void deleteNote(Long id) {
        try {
            noteRepository.delete(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete note with id " + id, e);
        }
    }
}
