package dev.nonvocal.notes.tui;

/**
 * Entry point for the TUI client.
 */
public class TuiMain {

    public static void main(String[] args) {
        try {
            TuiClient client = new TuiClient();
            client.start();
        } catch (Exception e) {
            System.err.println("Failed to start TUI: " + e.getMessage());
            System.exit(1);
        }
    }
}

