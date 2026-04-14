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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main TUI application shell.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Terminal / screen / GUI lifecycle</li>
 *   <li>Main window layout (sidebar + content area)</li>
 *   <li>Vim-style command bar (ESC)</li>
 *   <li>Delegating content rendering to {@link AllNotesView}, {@link NoteDetailDialog},
 *       {@link NoteFormDialog} and {@link SearchDialog}</li>
 * </ul>
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
 */
public class TuiClient {

    private static final int SIDEBAR_WIDTH = 22;

    private final NotesRestClient apiClient;
    private final Screen screen;
    private final MultiWindowTextGUI gui;

    /** Cached note list – refreshed on every {@link #showAllNotes()} call. */
    private List<Note> allNotes = new ArrayList<>();

    private Panel rootPanel;
    private BasicWindow mainWindow;
    private Panel contentArea;
    private final List<Button> sidebarButtons = new ArrayList<>();

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

        // ── Left sidebar ──────────────────────────────────────────────────────
        Panel sidebarInner = new Panel(new LinearLayout(Direction.VERTICAL));
        sidebarInner.addComponent(new EmptySpace(new TerminalSize(SIDEBAR_WIDTH - 2, 1)));
        addSidebarButton(sidebarInner, "All Notes",  this::showAllNotes);
        addSidebarButton(sidebarInner, "New Note…",  this::showNewNoteDialog);
        addSidebarButton(sidebarInner, "Search…",    this::showSearchDialog);
        sidebarInner.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        sidebarInner.addComponent(new Separator(Direction.HORIZONTAL));
        addSidebarButton(sidebarInner, "Exit", mainWindow::close);
        rootPanel.addComponent(
                sidebarInner.withBorder(Borders.singleLine("Notes")),
                BorderLayout.Location.LEFT);

        // ── Content area ──────────────────────────────────────────────────────
        contentArea = new Panel(new LinearLayout(Direction.VERTICAL));
        showWelcome();
        rootPanel.addComponent(
                contentArea.withBorder(Borders.singleLine("")),
                BorderLayout.Location.CENTER);

        // ── Command bar (hidden; added to BOTTOM on ESC) ──────────────────────
        this.commandBox = new TextBox(new TerminalSize(1, 1));
        this.commandBarPanel = new Panel(new BorderLayout());
        commandBarPanel.addComponent(new Label(":"), BorderLayout.Location.LEFT);
        commandBarPanel.addComponent(commandBox,     BorderLayout.Location.CENTER);

        mainWindow.setComponent(rootPanel);
        mainWindow.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                handleInput(keyStroke, deliverEvent);
            }
        });

        gui.addWindowAndWait(mainWindow);
        screen.stopScreen();
    }

    // ── Input handling ────────────────────────────────────────────────────────

    private void handleInput(KeyStroke keyStroke, AtomicBoolean deliverEvent) {
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
        if (!sidebarButtons.isEmpty()) sidebarButtons.get(0).takeFocus();
    }

    /**
     * Executes a Vim-style command.
     * Supported: {@code q/quit/exit}, {@code n/new}, {@code s/search}, {@code all/list/ls}.
     */
    private void handleCommand(String cmd) {
        switch (cmd.toLowerCase()) {
            case "q": case "quit": case "exit": mainWindow.close();      break;
            case "n": case "new":               showNewNoteDialog();      break;
            case "s": case "search":            showSearchDialog();       break;
            case "all": case "list": case "ls": showAllNotes();           break;
            default: break;
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
        allNotes = new AllNotesView(apiClient, this::showNoteDetail).populate(contentArea);
    }

    private void showNoteDetail(Note note) {
        new NoteDetailDialog(gui, apiClient, note, allNotes, this::showAllNotes).show();
    }

    private void showNewNoteDialog() {
        new NoteFormDialog(gui, apiClient, null, this::showAllNotes).show();
    }

    private void showSearchDialog() {
        if (allNotes.isEmpty()) {
            try { allNotes = apiClient.getAllNotes(); } catch (Exception ignored) {}
        }
        new SearchDialog(gui, allNotes, this::showNoteDetail).show();
    }
}
