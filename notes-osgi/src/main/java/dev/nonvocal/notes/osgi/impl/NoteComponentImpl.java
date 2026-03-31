package dev.nonvocal.notes.osgi.impl;

import dev.nonvocal.notes.core.entity.Note;
import dev.nonvocal.notes.core.service.NoteService;
import dev.nonvocal.notes.osgi.api.NoteComponent;
import org.osgi.service.component.annotations.*;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;

/**
 * Declarative Services (DS) component that implements {@link NoteComponent}.
 *
 * <p>The {@code @Component} annotation lets bnd-maven-plugin generate the
 * {@code OSGI-INF/dev.nonvocal.notes.osgi.impl.NoteComponentImpl.xml}
 * descriptor automatically at build time.</p>
 *
 * <p>The {@code @Reference} on {@link #setLogService} and
 * {@link #setNoteService} instructs the SCR runtime to inject the
 * matching OSGi services before {@link #activate()} is called.</p>
 */
@Component(
        name        = "dev.nonvocal.notes.NoteComponent",
        service     = NoteComponent.class,
        immediate   = true,
        property    = {
                "notes.version=1.0.0"
        }
)
public class NoteComponentImpl implements NoteComponent {

    // -----------------------------------------------------------------------
    // Injected OSGi services
    // -----------------------------------------------------------------------

    private volatile NoteService noteService;
    private volatile Logger      log;

    /**
     * Binds the {@link NoteService} provided by notes-core.
     * The service is mandatory (default policy) – the component will not
     * activate unless this service is present in the registry.
     */
    @Reference
    protected void setNoteService(NoteService noteService) {
        this.noteService = noteService;
    }

    protected void unsetNoteService(NoteService noteService) {
        this.noteService = null;
    }

    /**
     * Binds the OSGi {@link LogService} (optional).
     * Uses {@code cardinality = OPTIONAL} so the component starts even
     * without a log service, and {@code policy = DYNAMIC} so it can be
     * swapped at runtime without restarting the component.
     */
    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy      = ReferencePolicy.DYNAMIC
    )
    protected void setLogService(LogService logService) {
        this.log = logService.getLogger(NoteComponentImpl.class);
    }

    protected void unsetLogService(LogService logService) {
        this.log = null;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Called by the SCR after all mandatory references have been bound.
     * Use {@code @Activate} to perform initialisation work.
     */
    @Activate
    protected void activate() {
        info("NoteComponent activated.");
    }

    /**
     * Called by the SCR before the component is deactivated.
     */
    @Deactivate
    protected void deactivate() {
        info("NoteComponent deactivated.");
    }

    /**
     * Called when the component configuration is updated (e.g. via
     * ConfigurationAdmin) without a full restart.
     */
    @Modified
    protected void modified() {
        info("NoteComponent configuration modified.");
    }

    // -----------------------------------------------------------------------
    // NoteComponent implementation
    // -----------------------------------------------------------------------

    @Override
    public Note create(Note note) {
        info("Creating note: " + note.getTitle());
        return noteService.createNote(note);
    }

    @Override
    public Note get(long id) {
        return noteService.getNote(id);
    }

    @Override
    public void delete(long id) {
        info("Deleting note with id: " + id);
        noteService.deleteNote(id);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private void info(String msg) {
        Logger l = log;
        if (l != null) l.info(msg);
        else System.out.println("[NoteComponent] " + msg);
    }
}

