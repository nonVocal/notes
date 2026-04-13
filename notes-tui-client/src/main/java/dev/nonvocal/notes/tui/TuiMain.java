package dev.nonvocal.notes.tui;

/**
 * Entry point for the TUI client.
 *
 * <p>Usage: {@code java -jar notes-tui-client.jar [BASE_URL]}
 * <br>Default BASE_URL: {@value NotesRestClient#DEFAULT_BASE_URL}
 */
public class TuiMain {

    public static void main(String[] args) {
        String baseUrl = args.length > 0 ? args[0] : NotesRestClient.DEFAULT_BASE_URL;
        try {
            NotesRestClient apiClient = new NotesRestClient(baseUrl);
            TuiClient client = new TuiClient(apiClient);
            client.start();
        } catch (Exception e) {
            System.err.println("Failed to start TUI: " + e.getMessage());
            System.exit(1);
        }
    }
}
