package dev.nonvocal.notes.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import dev.nonvocal.notes.core.entity.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Populates a content panel with the list of all notes fetched from the REST API.
 * Returns the loaded notes so the caller can cache them.
 */
public class AllNotesView {

    private final NotesRestClient apiClient;
    private final Consumer<Note> onNoteSelected;

    public AllNotesView(NotesRestClient apiClient, Consumer<Note> onNoteSelected) {
        this.apiClient = apiClient;
        this.onNoteSelected = onNoteSelected;
    }

    /**
     * Fetches notes, fills {@code contentArea}, and returns the fetched list.
     * Returns an empty list on error.
     */
    public List<Note> populate(Panel contentArea) {
        contentArea.removeAllComponents();
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        contentArea.addComponent(new Label("All Notes"));
        contentArea.addComponent(new Separator(Direction.HORIZONTAL));
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        List<Note> notes;
        try {
            notes = apiClient.getAllNotes();
        } catch (Exception e) {
            contentArea.addComponent(new Label("Error loading notes: " + e.getMessage()));
            return new ArrayList<>();
        }

        if (notes.isEmpty()) {
            contentArea.addComponent(new Label("(No notes yet – press New Note… to create one)"));
            return notes;
        }

        ActionListBox listBox = new ActionListBox(new TerminalSize(90, 25));
        for (Note note : notes) {
            String entry = String.format("%-30s  %s",
                    TuiUtils.truncate(note.getTitle(), 30),
                    buildPreview(note));
            listBox.addItem(entry, () -> onNoteSelected.accept(note));
        }
        contentArea.addComponent(new Label(
                String.format("%-30s  %s  (Enter/click to open)", "TITLE", "PREVIEW")));
        contentArea.addComponent(listBox);
        return notes;
    }

    private String buildPreview(Note note) {
        if (note.getContent() == null || note.getContent().isBlank()) return "";
        String flat = note.getContent().replaceAll("\\s+", " ");
        return flat.substring(0, Math.min(flat.length(), 50)) + (flat.length() > 50 ? "…" : "");
    }
}

