package dev.nonvocal.notes.osgi.api;

import dev.nonvocal.notes.core.entity.Note;

/**
 * OSGi service interface for the Notes component.
 * Other bundles can obtain this service via the OSGi Service Registry.
 */
public interface NoteComponent {

    /**
     * Create and persist a new note.
     *
     * @param note the note to create (must not be {@code null})
     * @return the persisted note (with generated id)
     */
    Note create(Note note);

    /**
     * Retrieve a note by its unique identifier.
     *
     * @param id the note id
     * @return the note, or {@code null} if not found
     */
    Note get(long id);

    /**
     * Delete the note with the given id.
     *
     * @param id the note id
     */
    void delete(long id);
}

