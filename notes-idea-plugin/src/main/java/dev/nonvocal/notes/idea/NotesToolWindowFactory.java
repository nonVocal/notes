package dev.nonvocal.notes.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Factory for the Notes tool window.
 * Registered in plugin.xml under {@code <toolWindow>}.
 */
public class NotesToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        NotesPanel notesPanel = new NotesPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(notesPanel.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    // -----------------------------------------------------------------------
    // Inner panel class
    // -----------------------------------------------------------------------

    private static class NotesPanel {
        private final JPanel root;

        NotesPanel(Project project) {
            root = new JPanel(new BorderLayout());

            // Toolbar
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton newBtn    = new JButton("New");
            JButton deleteBtn = new JButton("Delete");
            JButton refreshBtn = new JButton("Refresh");
            toolbar.add(newBtn);
            toolbar.add(deleteBtn);
            toolbar.add(refreshBtn);

            // Note list
            DefaultListModel<String> listModel = new DefaultListModel<>();
            listModel.addElement("Loading notes…");
            JList<String> noteList = new JList<>(listModel);
            noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Detail area
            JTextArea detailArea = new JTextArea();
            detailArea.setEditable(false);
            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(noteList),
                    new JScrollPane(detailArea)
            );
            splitPane.setDividerLocation(180);

            root.add(toolbar,   BorderLayout.NORTH);
            root.add(splitPane, BorderLayout.CENTER);
        }

        JComponent getComponent() {
            return root;
        }
    }
}

