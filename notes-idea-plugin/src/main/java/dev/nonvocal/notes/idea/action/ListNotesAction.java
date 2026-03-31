package dev.nonvocal.notes.idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open / focus the Notes tool window.
 */
public class ListNotesAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        ToolWindow toolWindow = ToolWindowManager
                .getInstance(e.getProject())
                .getToolWindow("Notes");
        if (toolWindow != null) {
            toolWindow.show();
        }
    }
}

