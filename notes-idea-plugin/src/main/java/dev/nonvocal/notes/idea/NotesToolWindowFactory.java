package dev.nonvocal.notes.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Factory for the Notes tool window (sidebar).
 *
 * <p>Layout mirrors the TUI client:
 * <pre>
 * ┌─ 📝 Notes ──┬──────────────────────────────┐
 * │  All Notes  │  (content area – CardLayout) │
 * │  New Note   │                              │
 * │  Search     │                              │
 * └─────────────┴──────────────────────────────┘
 * </pre>
 *
 * <p>Call {@link #buildNotesPanel(Project)} from other actions to reuse the
 * same two-panel layout inside a dialog window.
 */
public class NotesToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(buildNotesPanel(project), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    // -----------------------------------------------------------------------
    // Public factory – reused by NotesDialogAction
    // -----------------------------------------------------------------------

    /** Creates the two-panel Notes UI component (sidebar nav + card content). */
    public static JComponent buildNotesPanel(Project project) {
        return new NotesPanel(project).getComponent();
    }

    // -----------------------------------------------------------------------
    // Inner panel
    // -----------------------------------------------------------------------

    static final class NotesPanel {

        private static final Color SIDEBAR_BG     = new Color(0x1a1a2e);
        private static final Color NAV_FG_IDLE    = new Color(0xb0b0cc);
        private static final Color NAV_BG_ACTIVE  = new Color(0x26265a);
        private static final Color ACCENT_BLUE    = new Color(0x4f8ef7);
        private static final Color SEPARATOR      = new Color(0x2e2e48);

        private final JPanel     root        = new JPanel(new BorderLayout());
        private final CardLayout cards       = new CardLayout();
        private final JPanel     contentPane = new JPanel(cards);
        private       JButton    activeBtn   = null;

        // Card names
        private static final String VIEW_ALL    = "all-notes";
        private static final String VIEW_NEW    = "new-note";
        private static final String VIEW_SEARCH = "search";

        NotesPanel(Project project) {
            // ── Left sidebar ──────────────────────────────────────────────
            JPanel sidebar = new JPanel(new BorderLayout());
            sidebar.setBackground(SIDEBAR_BG);
            sidebar.setPreferredSize(new Dimension(148, 0));

            JLabel title = new JLabel("  📝 Notes");
            title.setForeground(Color.WHITE);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
            title.setOpaque(true);
            title.setBackground(SIDEBAR_BG);
            title.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, SEPARATOR),
                    new EmptyBorder(11, 6, 11, 6)));

            JPanel nav = new JPanel();
            nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
            nav.setBackground(SIDEBAR_BG);
            nav.setBorder(new EmptyBorder(6, 0, 0, 0));

            JButton allBtn    = createNavButton("🗒  All Notes");
            JButton newBtn    = createNavButton("✏️  New Note");
            JButton searchBtn = createNavButton("🔍  Search");

            nav.add(allBtn);
            nav.add(newBtn);
            nav.add(searchBtn);

            JLabel hint = new JLabel("  Ctrl+F — Search");
            hint.setForeground(new Color(0x55556a));
            hint.setFont(hint.getFont().deriveFont(10f));
            hint.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARATOR),
                    new EmptyBorder(6, 6, 6, 6)));
            hint.setOpaque(true);
            hint.setBackground(SIDEBAR_BG);

            sidebar.add(title, BorderLayout.NORTH);
            sidebar.add(nav,   BorderLayout.CENTER);
            sidebar.add(hint,  BorderLayout.SOUTH);

            // ── Content cards ─────────────────────────────────────────────
            contentPane.add(buildAllNotesCard(),    VIEW_ALL);
            contentPane.add(buildNewNoteCard(),     VIEW_NEW);
            contentPane.add(buildSearchCard(),      VIEW_SEARCH);

            // ── Nav actions ───────────────────────────────────────────────
            allBtn   .addActionListener(e -> showCard(VIEW_ALL,    allBtn));
            newBtn   .addActionListener(e -> showCard(VIEW_NEW,    newBtn));
            searchBtn.addActionListener(e -> showCard(VIEW_SEARCH, searchBtn));

            // ── Split pane ────────────────────────────────────────────────
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, contentPane);
            split.setDividerSize(1);
            split.setDividerLocation(148);
            split.setResizeWeight(0);
            root.add(split, BorderLayout.CENTER);

            // ── Ctrl+F → search (when panel has focus) ────────────────────
            root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "ctrlF");
            root.getActionMap().put("ctrlF", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    showCard(VIEW_SEARCH, searchBtn);
                }
            });

            showCard(VIEW_ALL, allBtn);
        }

        // ── Nav button factory ─────────────────────────────────────────────

        private static JButton createNavButton(String text) {
            JButton btn = new JButton(text);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setBackground(SIDEBAR_BG);
            btn.setForeground(NAV_FG_IDLE);
            btn.setFont(btn.getFont().deriveFont(12f));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, SIDEBAR_BG),
                    new EmptyBorder(7, 9, 7, 9)));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setContentAreaFilled(false);
            btn.setOpaque(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setFocusPainted(false);
            return btn;
        }

        private void showCard(String name, JButton btn) {
            // Reset previously active button
            if (activeBtn != null) {
                activeBtn.setBackground(SIDEBAR_BG);
                activeBtn.setForeground(NAV_FG_IDLE);
                activeBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, SIDEBAR_BG),
                        new EmptyBorder(7, 9, 7, 9)));
            }
            // Activate new button
            activeBtn = btn;
            btn.setBackground(NAV_BG_ACTIVE);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT_BLUE),
                    new EmptyBorder(7, 9, 7, 9)));
            cards.show(contentPane, name);
        }

        // ── Card builders ──────────────────────────────────────────────────

        private static JComponent buildAllNotesCard() {
            JPanel panel = new JPanel(new BorderLayout());
            JLabel heading = cardTitle("All Notes");

            DefaultListModel<String> model = new DefaultListModel<>();
            model.addElement("Loading notes…");
            JList<String> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            panel.add(heading,             BorderLayout.NORTH);
            panel.add(new JScrollPane(list), BorderLayout.CENTER);
            return panel;
        }

        private static JComponent buildNewNoteCard() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new EmptyBorder(12, 14, 12, 14));

            JPanel form = new JPanel();
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

            JTextField titleField = new JTextField();
            titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JTextArea contentArea = new JTextArea(5, 20);
            contentArea.setLineWrap(true);
            contentArea.setWrapStyleWord(true);

            JButton saveBtn = new JButton("Save Note");
            saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            // TODO: wire up save button to NoteService / REST API

            form.add(fieldLabel("Title"));
            form.add(Box.createRigidArea(new Dimension(0, 3)));
            form.add(titleField);
            form.add(Box.createRigidArea(new Dimension(0, 10)));
            form.add(fieldLabel("Content"));
            form.add(Box.createRigidArea(new Dimension(0, 3)));
            form.add(new JScrollPane(contentArea));
            form.add(Box.createRigidArea(new Dimension(0, 10)));
            form.add(saveBtn);

            panel.add(cardTitle("New Note"), BorderLayout.NORTH);
            panel.add(form,                  BorderLayout.CENTER);
            return panel;
        }

        private static JComponent buildSearchCard() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new EmptyBorder(12, 14, 12, 14));

            JTextField searchField = new JTextField();
            searchField.putClientProperty("JTextField.placeholderText", "Search notes…");

            JPanel results = new JPanel(new BorderLayout());
            results.add(new JLabel("Type to search…"), BorderLayout.NORTH);
            // TODO: wire up search field to NoteService / REST API

            JPanel top = new JPanel(new BorderLayout(0, 8));
            top.add(cardTitle("Search"), BorderLayout.NORTH);
            top.add(searchField,         BorderLayout.SOUTH);

            panel.add(top,                   BorderLayout.NORTH);
            panel.add(new JScrollPane(results), BorderLayout.CENTER);
            return panel;
        }

        // ── Small helpers ──────────────────────────────────────────────────

        private static JLabel cardTitle(String text) {
            JLabel label = new JLabel(text);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xdde1e8)),
                    new EmptyBorder(10, 14, 10, 14)));
            return label;
        }

        private static JLabel fieldLabel(String text) {
            JLabel label = new JLabel(text);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
            label.setForeground(new Color(0x444444));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            return label;
        }

        JComponent getComponent() { return root; }
    }
}


