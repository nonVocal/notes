package dev.nonvocal.notes.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import dev.nonvocal.notes.core.entity.Note;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modal form for creating a new note (existingNote == null)
 * or editing an existing one (existingNote != null).
 */
public class NoteFormDialog {

    private final MultiWindowTextGUI gui;
    private final NotesRestClient apiClient;
    private final Note existingNote;   // null → create mode
    private final Runnable onSuccess;

    public NoteFormDialog(MultiWindowTextGUI gui, NotesRestClient apiClient,
                          Note existingNote, Runnable onSuccess) {
        this.gui = gui;
        this.apiClient = apiClient;
        this.existingNote = existingNote;
        this.onSuccess = onSuccess;
    }

    public void show() {
        boolean editMode = existingNote != null;
        BasicWindow dialog = new BasicWindow(editMode
                ? "Edit Note #" + existingNote.getId()
                : "New Note");
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Title:"));
        TextBox titleBox = new TextBox(new TerminalSize(60, 1));
        if (editMode) titleBox.setText(existingNote.getTitle() != null ? existingNote.getTitle() : "");
        panel.addComponent(titleBox);

        panel.addComponent(new Label("Content:"));
        TextBox contentBox = new TextBox(new TerminalSize(60, 8), TextBox.Style.MULTI_LINE);
        if (editMode) contentBox.setText(existingNote.getContent() != null ? existingNote.getContent() : "");
        panel.addComponent(contentBox);

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        Label statusLabel = new Label("");
        panel.addComponent(statusLabel);

        Panel btnRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        btnRow.addComponent(new Button("Save", () -> {
            String title = titleBox.getText().trim();
            if (title.isEmpty()) {
                statusLabel.setText("Title cannot be empty.");
                return;
            }
            Note note = new Note();
            note.setTitle(title);
            note.setContent(contentBox.getText());
            try {
                if (editMode) {
                    apiClient.updateNote(existingNote.getId(), note);
                } else {
                    apiClient.createNote(note);
                }
                dialog.close();
                onSuccess.run();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        }));
        btnRow.addComponent(new Button("Cancel", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);
        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window w, KeyStroke ks, AtomicBoolean deliver) {
                if (ks.getKeyType() == KeyType.Escape) {
                    deliver.set(false);
                    dialog.close();
                }
            }
        });

        gui.addWindowAndWait(dialog);
    }
}

