package dev.nonvocal.notes.idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import dev.nonvocal.notes.idea.NotesToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Action that opens the Notes two-panel UI in a floating, non-modal dialog
 * window – the "dialog" counterpart to the persistent tool-window sidebar.
 *
 * <p>The dialog reuses {@link NotesToolWindowFactory#buildNotesPanel(Project)}
 * so both surfaces always share the same layout.
 *
 * <p>Registered in {@code plugin.xml} and bound to {@code Ctrl+Alt+N}.
 */
public class NotesDialogAction extends AnAction {

    /** Singleton dialog – re-focus instead of opening a second copy. */
    private JDialog dialog;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // If the dialog is already open, just bring it to the front.
        if (dialog != null && dialog.isVisible()) {
            dialog.toFront();
            dialog.requestFocus();
            return;
        }

        Frame parentFrame = project != null
                ? WindowManager.getInstance().getFrame(project)
                : null;

        dialog = new JDialog(parentFrame, "Notes", false /* non-modal */);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setSize(760, 520);
        dialog.setMinimumSize(new Dimension(500, 360));
        dialog.setLocationRelativeTo(parentFrame);

        // Reuse the same two-panel component as the tool window
        dialog.getContentPane().add(NotesToolWindowFactory.buildNotesPanel(project));

        // ESC closes the dialog
        dialog.getRootPane().registerKeyboardAction(
                ev -> dialog.setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setVisible(true);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
    }
}

