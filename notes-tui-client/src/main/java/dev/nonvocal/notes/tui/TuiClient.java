package dev.nonvocal.notes.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.input.MouseAction;
import com.googlecode.lanterna.input.MouseActionType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.MouseCaptureMode;
import com.googlecode.lanterna.terminal.Terminal;
import dev.nonvocal.notes.core.entity.Note;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Terminal UI for the Notes application using Lanterna.
 *
 * <p>Layout:
 * <pre>
 * ┌──[ Notes ]──┬─────────────────────────────────┐
 * │ All Notes   │                                 │
 * │ New Note…   │   (content area)                │
 * │─────────────│                                 │
 * │ Exit        │                                 │
 * ├─────────────┴─────────────────────────────────┤
 * │ :_                          (command bar, ESC)│
 * └───────────────────────────────────────────────┘
 * </pre>
 * The sidebar is always visible; the content area updates on selection.
 * Press ESC to open the Vim-style command bar (ENTER executes, ESC cancels).
 */
public class TuiClient {

    /** Fixed width (in columns) of the left sidebar including its border. */
    private static final int SIDEBAR_WIDTH = 22;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotesRestClient apiClient;
    private final Screen screen;
    private final MultiWindowTextGUI gui;

    /** Cached note list – refreshed on every showAllNotes() call. */
    private List<Note> allNotes = new ArrayList<>();

    // ── Main layout references (needed across methods) ────────────────────────
    private Panel rootPanel;
    private BasicWindow mainWindow;

    /** The centre panel whose content is swapped when the user picks a sidebar item. */
    private Panel contentArea;

    /**
     * Sidebar navigation buttons kept so the scroll wheel can move focus between them.
     * Order matches top-to-bottom visual order.
     */
    private final List<Button> sidebarButtons = new ArrayList<>();

    // ── Command bar (Vim-style, shown on ESC) ─────────────────────────────────
    private Panel commandBarPanel;
    private TextBox commandBox;
    private boolean commandBarVisible = false;

    public TuiClient(NotesRestClient apiClient) throws IOException {
        this.apiClient = apiClient;
        Terminal terminal = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(120, 40))
                .setMouseCaptureMode(MouseCaptureMode.CLICK_RELEASE)
                .createTerminal();
        this.screen = new TerminalScreen(terminal);
        this.screen.startScreen();
        this.gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                new EmptySpace(TextColor.ANSI.BLACK_BRIGHT));
    }

    public void start() throws IOException {
        this.mainWindow = new BasicWindow("Notes TUI");
        mainWindow.setHints(List.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));

        this.rootPanel = new Panel(new BorderLayout());

        // ── Left sidebar ─────────────────────────────────────────────────────
        Panel sidebarInner = new Panel(new LinearLayout(Direction.VERTICAL));
        sidebarInner.addComponent(new EmptySpace(new TerminalSize(SIDEBAR_WIDTH - 2, 1)));

        addSidebarButton(sidebarInner, "All Notes",  this::showAllNotes);
        addSidebarButton(sidebarInner, "New Note…", this::showNewNoteDialog);
        addSidebarButton(sidebarInner, "Search…",   this::showSearchDialog);
        sidebarInner.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        sidebarInner.addComponent(new Separator(Direction.HORIZONTAL));
        addSidebarButton(sidebarInner, "Exit", mainWindow::close);

        rootPanel.addComponent(
                sidebarInner.withBorder(Borders.singleLine("Notes")),
                BorderLayout.Location.LEFT);

        // ── Content area ─────────────────────────────────────────────────────
        contentArea = new Panel(new LinearLayout(Direction.VERTICAL));
        showWelcome();
        rootPanel.addComponent(
                contentArea.withBorder(Borders.singleLine("")),
                BorderLayout.Location.CENTER);

        // ── Command bar (hidden; added to BOTTOM dynamically on ESC) ─────────
        this.commandBox = new TextBox(new TerminalSize(1, 1));
        this.commandBarPanel = new Panel(new BorderLayout());
        commandBarPanel.addComponent(new Label(":"), BorderLayout.Location.LEFT);
        commandBarPanel.addComponent(commandBox,     BorderLayout.Location.CENTER);

        mainWindow.setComponent(rootPanel);

        // ── Global keyboard shortcuts + mouse scroll ──────────────────────────
        mainWindow.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                KeyType type = keyStroke.getKeyType();

                if (commandBarVisible) {
                    if (type == KeyType.Enter) {
                        deliverEvent.set(false);
                        String cmd = commandBox.getText().trim();
                        hideCommandBar();
                        handleCommand(cmd);
                    } else if (type == KeyType.Escape) {
                        deliverEvent.set(false);
                        hideCommandBar();
                    }
                    return;
                }

                if (type == KeyType.Escape) {
                    deliverEvent.set(false);
                    showCommandBar();
                    return;
                }
                if (type == KeyType.Character
                        && keyStroke.getCharacter() == 'f'
                        && keyStroke.isCtrlDown()) {
                    deliverEvent.set(false);
                    showSearchDialog();
                    return;
                }
                if (keyStroke instanceof MouseAction) {
                    MouseActionType mtype = ((MouseAction) keyStroke).getActionType();
                    if (mtype == MouseActionType.SCROLL_UP) {
                        deliverEvent.set(false);
                        moveSidebarFocus(-1);
                    } else if (mtype == MouseActionType.SCROLL_DOWN) {
                        deliverEvent.set(false);
                        moveSidebarFocus(+1);
                    }
                }
            }
        });

        gui.addWindowAndWait(mainWindow);
        screen.stopScreen();
    }

    // ── Command bar ───────────────────────────────────────────────────────────

    private void showCommandBar() {
        if (commandBarVisible) return;
        commandBarVisible = true;
        commandBox.setText("");
        rootPanel.addComponent(commandBarPanel, BorderLayout.Location.BOTTOM);
        commandBox.takeFocus();
    }

    private void hideCommandBar() {
        if (!commandBarVisible) return;
        commandBarVisible = false;
        rootPanel.removeComponent(commandBarPanel);
        if (!sidebarButtons.isEmpty()) {
            sidebarButtons.get(0).takeFocus();
        }
    }

    /**
     * Executes a command entered in the command bar.
     * Supported commands (case-insensitive):
     * <ul>
     *   <li>{@code q / quit / exit}  – quit the application</li>
     *   <li>{@code new / n}          – open the New Note dialog</li>
     *   <li>{@code search / s}       – open the Search dialog</li>
     *   <li>{@code all / list / ls}  – show All Notes view</li>
     * </ul>
     */
    private void handleCommand(String cmd) {
        switch (cmd.toLowerCase()) {
            case "q": case "quit": case "exit":
                mainWindow.close();
                break;
            case "new": case "n":
                showNewNoteDialog();
                break;
            case "search": case "s":
                showSearchDialog();
                break;
            case "all": case "list": case "ls":
                showAllNotes();
                break;
            default:
                break;
        }
    }

    // ── Sidebar helpers ───────────────────────────────────────────────────────

    private void addSidebarButton(Panel sidebar, String label, Runnable action) {
        Button btn = new Button(label, action);
        btn.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        sidebar.addComponent(btn);
        sidebarButtons.add(btn);
    }

    private void moveSidebarFocus(int delta) {
        if (sidebarButtons.isEmpty()) return;
        int current = -1;
        for (int i = 0; i < sidebarButtons.size(); i++) {
            if (sidebarButtons.get(i).isFocused()) { current = i; break; }
        }
        int next = Math.floorMod((current == -1 ? 0 : current) + delta, sidebarButtons.size());
        sidebarButtons.get(next).takeFocus();
    }

    // ── Content views ─────────────────────────────────────────────────────────

    private void showWelcome() {
        contentArea.removeAllComponents();
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        contentArea.addComponent(new Label("Welcome to Notes TUI Client"));
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        contentArea.addComponent(new Label("API: " + apiClient.getBaseUrl()));
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        contentArea.addComponent(new Label("Select an option from the sidebar."));
    }

    private void showAllNotes() {
        contentArea.removeAllComponents();
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        contentArea.addComponent(new Label("All Notes"));
        contentArea.addComponent(new Separator(Direction.HORIZONTAL));
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        try {
            allNotes = apiClient.getAllNotes();
        } catch (Exception e) {
            contentArea.addComponent(new Label("Error loading notes: " + e.getMessage()));
            return;
        }

        if (allNotes.isEmpty()) {
            contentArea.addComponent(new Label("(No notes yet – press New Note… to create one)"));
            return;
        }

        ActionListBox listBox = new ActionListBox(new TerminalSize(90, 25));
        for (Note note : allNotes) {
            String preview = note.getContent() != null && !note.getContent().isBlank()
                    ? note.getContent().replaceAll("\\s+", " ").substring(
                            0, Math.min(note.getContent().replaceAll("\\s+", " ").length(), 50))
                            + (note.getContent().length() > 50 ? "…" : "")
                    : "";
            String entry = String.format("%-30s  %s", truncate(note.getTitle(), 30), preview);
            Note captured = note;
            listBox.addItem(entry, () -> showNoteDetail(captured));
        }
        contentArea.addComponent(new Label(
                String.format("%-30s  %s  (Enter/click to open)", "TITLE", "PREVIEW")));
        contentArea.addComponent(listBox);
    }

    // ── Note detail dialog ────────────────────────────────────────────────────

    private void showNoteDetail(Note note) {
        BasicWindow dialog = new BasicWindow("Note #" + note.getId());
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Title:   " + note.getTitle()));
        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        if (note.getContent() != null && !note.getContent().isBlank()) {
            panel.addComponent(new Label("Content:"));
            // Word-wrap at 70 chars
            for (String line : wrapText(note.getContent(), 70)) {
                panel.addComponent(new Label("  " + line));
            }
        } else {
            panel.addComponent(new Label("Content: (empty)"));
        }

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        panel.addComponent(new Label("Created:  " + (note.getCreatedAt()  != null ? note.getCreatedAt().format(DT_FMT)  : "—")));
        panel.addComponent(new Label("Modified: " + (note.getModifiedAt() != null ? note.getModifiedAt().format(DT_FMT) : "—")));

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        Panel btnRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        btnRow.addComponent(new Button("Edit", () -> {
            dialog.close();
            showEditNoteDialog(note);
        }));
        btnRow.addComponent(new Button("Delete", () -> {
            dialog.close();
            confirmDelete(note);
        }));
        btnRow.addComponent(new Button("Close", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);

        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                if (keyStroke.getKeyType() == KeyType.Escape) {
                    deliverEvent.set(false);
                    dialog.close();
                }
            }
        });

        gui.addWindowAndWait(dialog);
    }

    // ── Delete confirmation ───────────────────────────────────────────────────

    private void confirmDelete(Note note) {
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
                showAllNotes();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        }));
        btnRow.addComponent(new Button("Cancel", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);
        gui.addWindowAndWait(dialog);
    }

    // ── Edit note dialog ──────────────────────────────────────────────────────

    private void showEditNoteDialog(Note note) {
        BasicWindow dialog = new BasicWindow("Edit Note #" + note.getId());
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Title:"));
        TextBox titleBox = new TextBox(new TerminalSize(60, 1));
        titleBox.setText(note.getTitle() != null ? note.getTitle() : "");
        panel.addComponent(titleBox);

        panel.addComponent(new Label("Content:"));
        TextBox contentBox = new TextBox(new TerminalSize(60, 8), TextBox.Style.MULTI_LINE);
        contentBox.setText(note.getContent() != null ? note.getContent() : "");
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
            Note updated = new Note();
            updated.setTitle(title);
            updated.setContent(contentBox.getText());
            try {
                apiClient.updateNote(note.getId(), updated);
                dialog.close();
                showAllNotes();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        }));
        btnRow.addComponent(new Button("Cancel", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);

        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                if (keyStroke.getKeyType() == KeyType.Escape) {
                    deliverEvent.set(false);
                    dialog.close();
                }
            }
        });

        gui.addWindowAndWait(dialog);
    }

    // ── New note dialog ───────────────────────────────────────────────────────

    private void showNewNoteDialog() {
        BasicWindow dialog = new BasicWindow("New Note");
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Title:"));
        TextBox titleBox = new TextBox(new TerminalSize(60, 1));
        panel.addComponent(titleBox);

        panel.addComponent(new Label("Content:"));
        TextBox contentBox = new TextBox(new TerminalSize(60, 8), TextBox.Style.MULTI_LINE);
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
                apiClient.createNote(note);
                dialog.close();
                showAllNotes();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
            }
        }));
        btnRow.addComponent(new Button("Cancel", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);

        dialog.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                if (keyStroke.getKeyType() == KeyType.Escape) {
                    deliverEvent.set(false);
                    dialog.close();
                }
            }
        });

        gui.addWindowAndWait(dialog);
    }

    // ── Search dialog ─────────────────────────────────────────────────────────

    private void showSearchDialog() {
        // Ensure the local cache is fresh
        if (allNotes.isEmpty()) {
            try {
                allNotes = apiClient.getAllNotes();
            } catch (Exception ignored) { /* handled inside dialog */ }
        }

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
            List<Note> hits = allNotes.stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(query)
                            || (n.getContent() != null && n.getContent().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
            if (hits.isEmpty()) {
                resultsInner.addComponent(new Label("No results for: \"" + query + "\""));
            } else {
                ActionListBox resultList = new ActionListBox(new TerminalSize(60, 10));
                for (Note n : hits) {
                    Note captured = n;
                    resultList.addItem(truncate(n.getTitle(), 58), () -> {
                        dialog.close();
                        showNoteDetail(captured);
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
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                KeyType type = keyStroke.getKeyType();
                if (type == KeyType.Escape) {
                    deliverEvent.set(false);
                    dialog.close();
                } else if (type == KeyType.Enter) {
                    // Let Enter trigger search when the search box is focused
                    if (searchBox.isFocused()) {
                        deliverEvent.set(false);
                        doSearch.run();
                    }
                }
            }
        });

        gui.addWindowAndWait(dialog);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : text.split("\n", -1)) {
            if (rawLine.length() <= width) {
                lines.add(rawLine);
            } else {
                String remaining = rawLine;
                while (remaining.length() > width) {
                    int cut = remaining.lastIndexOf(' ', width);
                    if (cut <= 0) cut = width;
                    lines.add(remaining.substring(0, cut));
                    remaining = remaining.substring(cut).stripLeading();
                }
                if (!remaining.isEmpty()) lines.add(remaining);
            }
        }
        return lines;
    }
}
