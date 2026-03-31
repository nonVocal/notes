package dev.nonvocal.notes.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.Arrays;

/**
 * Terminal UI for the Notes application using Lanterna.
 */
public class TuiClient {

    private final Screen screen;
    private final MultiWindowTextGUI gui;

    public TuiClient() throws IOException {
        Terminal terminal = new DefaultTerminalFactory()
                .setInitialTerminalSize(new TerminalSize(120, 40))
                .createTerminal();
        this.screen = new TerminalScreen(terminal);
        this.screen.startScreen();
        this.gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                new EmptySpace(TextColor.ANSI.BLUE));
    }

    public void start() throws IOException {
        BasicWindow window = new BasicWindow("Notes TUI");
        window.setHints(Arrays.asList(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Welcome to Notes TUI Client"));
        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

        panel.addComponent(new Button("New Note", () -> showNewNoteDialog(window)));
        panel.addComponent(new Button("Exit", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);
        screen.stopScreen();
    }

    private void showNewNoteDialog(BasicWindow parent) {
        BasicWindow dialog = new BasicWindow("New Note");
        dialog.setHints(Arrays.asList(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Title:"));
        TextBox titleBox = new TextBox(new TerminalSize(40, 1));
        panel.addComponent(titleBox);

        panel.addComponent(new Label("Content:"));
        TextBox contentBox = new TextBox(new TerminalSize(40, 5), TextBox.Style.MULTI_LINE);
        panel.addComponent(contentBox);

        panel.addComponent(new EmptySpace(new TerminalSize(0, 1)));
        panel.addComponent(new Button("Save", () -> {
            // Save note logic here
            dialog.close();
        }));
        panel.addComponent(new Button("Cancel", dialog::close));

        dialog.setComponent(panel);
        gui.addWindowAndWait(dialog);
    }
}

