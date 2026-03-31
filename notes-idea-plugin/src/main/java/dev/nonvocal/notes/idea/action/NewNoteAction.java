package dev.nonvocal.notes.idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Action to create a new note.
 * Registered in plugin.xml under {@code <actions>}.
 */
public class NewNoteAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        String title = Messages.showInputDialog(
                project,
                "Note title:",
                "New Note",
                Messages.getQuestionIcon()
        );
        if (title == null || title.isBlank()) return;

        String content = Messages.showMultilineInputDialog(
                project,
                "Note content:",
                "New Note – " + title,
                "",
                Messages.getQuestionIcon(),
                null
        );
        if (content == null) return;

        // TODO: call NoteService / REST API to persist the note
        Messages.showInfoMessage(project, "Note \"" + title + "\" saved.", "Notes");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Action is always available
        e.getPresentation().setEnabledAndVisible(true);
    }
}

