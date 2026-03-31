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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * └─────────────┴─────────────────────────────────┘
 * </pre>
 * The sidebar is always visible; the content area updates on selection.
 */
public class TuiClient {

    /** Fixed width (in columns) of the left sidebar including its border. */
    private static final int SIDEBAR_WIDTH = 22;

    private final Screen screen;
    private final MultiWindowTextGUI gui;

    /** The centre panel whose content is swapped when the user picks a sidebar item. */
    private Panel contentArea;

    /**
     * Sidebar navigation buttons kept so the scroll wheel can move focus between them.
     * Order matches top-to-bottom visual order.
     */
    private final java.util.List<Button> sidebarButtons = new java.util.ArrayList<>();

    public TuiClient() throws IOException {
        Terminal terminal = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(120, 40))
                // Enable click + release events so mouse clicks activate buttons/textboxes.
                // CLICK_RELEASE is the least intrusive mode; no constant move-event spam.
                .setMouseCaptureMode(MouseCaptureMode.CLICK_RELEASE)
                .createTerminal();
        this.screen = new TerminalScreen(terminal);
        this.screen.startScreen();
        this.gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                new EmptySpace(TextColor.ANSI.BLACK_BRIGHT));
    }

    public void start() throws IOException {
        // Full-screen, no window decorations – the inner borders provide structure.
        BasicWindow window = new BasicWindow("Notes TUI");
        window.setHints(List.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));

        Panel rootPanel = new Panel(new BorderLayout());

        // ── Left sidebar ─────────────────────────────────────────────────────
        Panel sidebarInner = new Panel(new LinearLayout(Direction.VERTICAL));
        // Invisible spacer that fixes the column width of the sidebar
        sidebarInner.addComponent(new EmptySpace(new TerminalSize(SIDEBAR_WIDTH - 2, 1)));

        addSidebarButton(sidebarInner, "All Notes",  this::showAllNotes);
        addSidebarButton(sidebarInner, "New Note…", this::showNewNoteDialog);
        addSidebarButton(sidebarInner, "Search…",   this::showSearchDialog);
        sidebarInner.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        sidebarInner.addComponent(new Separator(Direction.HORIZONTAL));
        addSidebarButton(sidebarInner, "Exit", window::close);

        rootPanel.addComponent(
                sidebarInner.withBorder(Borders.singleLine("Notes")),
                BorderLayout.Location.LEFT);

        // ── Content area ─────────────────────────────────────────────────────
        contentArea = new Panel(new LinearLayout(Direction.VERTICAL));
        showWelcome();
        rootPanel.addComponent(
                contentArea.withBorder(Borders.singleLine("")),
                BorderLayout.Location.CENTER);

        window.setComponent(rootPanel);

        // ── Global keyboard shortcuts + mouse scroll ──────────────────────────
        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
                // Ctrl+F → search dialog
                if (keyStroke.getKeyType() == KeyType.Character
                        && keyStroke.getCharacter() == 'f'
                        && keyStroke.isCtrlDown()) {
                    deliverEvent.set(false);
                    showSearchDialog();
                    return;
                }
                // Mouse scroll wheel → move sidebar focus up / down
                if (keyStroke instanceof MouseAction) {
                    MouseActionType type = ((MouseAction) keyStroke).getActionType();
                    if (type == MouseActionType.SCROLL_UP) {
                        deliverEvent.set(false);
                        moveSidebarFocus(-1);
                    } else if (type == MouseActionType.SCROLL_DOWN) {
                        deliverEvent.set(false);
                        moveSidebarFocus(+1);
                    }
                }
            }
        });

        gui.addWindowAndWait(window);
        screen.stopScreen();
    }

    // ── Sidebar helpers ───────────────────────────────────────────────────────

    /** Adds a full-width button to the sidebar and registers it for scroll navigation. */
    private void addSidebarButton(Panel sidebar, String label, Runnable action) {
        Button btn = new Button(label, action);
        btn.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        sidebar.addComponent(btn);
        sidebarButtons.add(btn);
    }

    /**
     * Moves keyboard focus within the sidebar by {@code delta} steps (−1 = up, +1 = down).
     * Wraps around at both ends.
     */
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
        contentArea.addComponent(new Label("Select an option from the sidebar."));
    }

    private void showAllNotes() {
        contentArea.removeAllComponents();
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        contentArea.addComponent(new Label("All Notes"));
        contentArea.addComponent(new Separator(Direction.HORIZONTAL));
        contentArea.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        // TODO: load notes from NoteService
        contentArea.addComponent(new Label("(No notes yet)"));
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showSearchDialog() {
        BasicWindow dialog = new BasicWindow("Search Notes  [Ctrl+F]");
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        // ── Input row ─────────────────────────────────────────────────────────
        Panel inputRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        inputRow.addComponent(new Label("Find: "));
        TextBox searchBox = new TextBox(new TerminalSize(34, 1));
        inputRow.addComponent(searchBox);
        panel.addComponent(inputRow);

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        // ── Results ───────────────────────────────────────────────────────────
        Panel resultsInner = new Panel(new LinearLayout(Direction.VERTICAL));
        resultsInner.addComponent(new Label("Type a term and press Search."));
        panel.addComponent(resultsInner.withBorder(Borders.singleLine("Results")));

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        // ── Buttons ───────────────────────────────────────────────────────────
        Panel btnRow = new Panel(new LinearLayout(Direction.HORIZONTAL));
        btnRow.addComponent(new Button("Search", () -> {
            String query = searchBox.getText().trim();
            resultsInner.removeAllComponents();
            if (query.isEmpty()) {
                resultsInner.addComponent(new Label("Enter a search term."));
            } else {
                // TODO: search via NoteService and show matching notes
                resultsInner.addComponent(new Label("No results for: \"" + query + "\""));
            }
        }));
        btnRow.addComponent(new Button("Close", dialog::close));
        panel.addComponent(btnRow);

        dialog.setComponent(panel);
        gui.addWindowAndWait(dialog);
    }

    private void showNewNoteDialog() {
        BasicWindow dialog = new BasicWindow("New Note");
        dialog.setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Title:"));
        TextBox titleBox = new TextBox(new TerminalSize(40, 1));
        panel.addComponent(titleBox);

        panel.addComponent(new Label("Content:"));
        TextBox contentBox = new TextBox(new TerminalSize(40, 5), TextBox.Style.MULTI_LINE);
        panel.addComponent(contentBox);

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        // Save note logic here
        panel.addComponent(new Button("Save",   dialog::close));
        panel.addComponent(new Button("Cancel", dialog::close));

        dialog.setComponent(panel);
        gui.addWindowAndWait(dialog);
    }
}

