package dev.nonvocal.notes.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import dev.nonvocal.notes.core.entity.Note;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Modal dialog that filters a list of notes by title or content. */
public class SearchDialog {

    private final MultiWindowTextGUI gui;
    private final List<Note> notes;
    private final Consumer<Note> onNoteSelected;

    public SearchDialog(MultiWindowTextGUI gui, List<Note> notes, Consumer<Note> onNoteSelected) {
        this.gui = gui;
        this.notes = notes;
        this.onNoteSelected = onNoteSelected;
    }

    public void show() {
        BasicWindow dialog = new BasicWindow("Search Notes  [Ctrl+F]");
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        Panel inputRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        inputRow.addComponent(new Label("Find: "));
        TextBox searchBox = new TextBox(new TerminalSize(46, 1));
        inputRow.addComponent(searchBox);
        panel.addComponent(inputRow);

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        Panel resultsInner = new Panel(new LinearLayout(Direction.VERTICAL));
        resultsInner.addComponent(new Label("Type a term and press Search or Enter."));
        panel.addComponent(resultsInner.withBorder(Borders.singleLine("Results")));

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        Runnable doSearch = () -> {
            String query = searchBox.getText().trim().toLowerCase();
            resultsInner.removeAllComponents();
            if (query.isEmpty()) {
                resultsInner.addComponent(new Label("Enter a search term."));
                return;
            }
            List<Note> hits = notes.stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(query)
                            || (n.getContent() != null && n.getContent().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
            if (hits.isEmpty()) {
                resultsInner.addComponent(new Label("No results for: \"" + query + "\""));
            } else {
                ActionListBox resultList = new ActionListBox(new TerminalSize(60, 10));
                for (Note n : hits) {
                    resultList.addItem(TuiUtils.truncate(n.getTitle(), 58), () -> {
                        dialog.close();
                        onNoteSelected.accept(n);
                    });
                }
                resultsInner.addComponent(resultList);
            }
        };

        Panel btnRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        btnRow.addComponent(new Button("Search", doSearch));
        btnRow.addComponent(new Button("Close", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);
        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window w, KeyStroke ks, AtomicBoolean deliver) {
                if (ks.getKeyType() == KeyType.Escape) {
                    deliver.set(false);
                    dialog.close();
                } else if (ks.getKeyType() == KeyType.Enter && searchBox.isFocused()) {
                    deliver.set(false);
                    doSearch.run();
                }
            }
        });

        gui.addWindowAndWait(dialog);
    }
}

