package dev.nonvocal.notes.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import dev.nonvocal.notes.core.entity.Note;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modal dialog that displays the full details of a single note.
 * Provides Edit and Delete actions; delete shows an inline confirmation.
 */
public class NoteDetailDialog {

    private final MultiWindowTextGUI gui;
    private final NotesRestClient apiClient;
    private final Note note;
    private final List<Note> allNotes;  // shared cache – delete removes from here
    private final Runnable onChanged;   // called after edit or delete to refresh the list

    public NoteDetailDialog(MultiWindowTextGUI gui, NotesRestClient apiClient,
                            Note note, List<Note> allNotes, Runnable onChanged) {
        this.gui = gui;
        this.apiClient = apiClient;
        this.note = note;
        this.allNotes = allNotes;
        this.onChanged = onChanged;
    }

    public void show() {
        BasicWindow dialog = new BasicWindow("Note #" + note.getId());
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Title:   " + note.getTitle()));
        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        if (note.getContent() != null && !note.getContent().isBlank()) {
            panel.addComponent(new Label("Content:"));
            for (String line : TuiUtils.wrapText(note.getContent(), 70)) {
                panel.addComponent(new Label("  " + line));
            }
        } else {
            panel.addComponent(new Label("Content: (empty)"));
        }

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        panel.addComponent(new Label("Created:  " + (note.getCreatedAt() != null
                ? note.getCreatedAt().format(TuiUtils.DT_FMT) : "—")));
        panel.addComponent(new Label("Modified: " + (note.getModifiedAt() != null
                ? note.getModifiedAt().format(TuiUtils.DT_FMT) : "—")));
        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        Panel btnRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        btnRow.addComponent(new Button("Edit", () -> {
            dialog.close();
            new NoteFormDialog(gui, apiClient, note, onChanged).show();
        }));
        btnRow.addComponent(new Button("Delete", () -> {
            dialog.close();
            showDeleteConfirmation();
        }));
        btnRow.addComponent(new Button("Close", dialog::close));
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

    private void showDeleteConfirmation() {
        BasicWindow dialog = new BasicWindow("Delete Note?");
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Delete \"" + note.getTitle() + "\"?"));
        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        Label statusLabel = new Label("");
        panel.addComponent(statusLabel);

        Panel btnRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        btnRow.addComponent(new Button("Yes, delete", () -> {
            try {
                apiClient.deleteNote(note.getId());
                allNotes.removeIf(n -> n.getId().equals(note.getId()));
                dialog.close();
                onChanged.run();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        }));
        btnRow.addComponent(new Button("Cancel", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);
        gui.addWindowAndWait(dialog);
    }
}

