package dev.nonvocal.notes.tui;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Static utility methods shared across TUI components. */
public final class TuiUtils {

    public static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TuiUtils() {}

    /** Truncates {@code s} to at most {@code max} characters, appending "…" if cut. */
    public static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Word-wraps {@code text} to lines of at most {@code width} characters. */
    public static List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : text.split("\n", -1)) {
            if (rawLine.length() <= width) {
                lines.add(rawLine);
            } else {
                String remaining = rawLine;
                while (remaining.length() > width) {
                    int cut = remaining.lastIndexOf(' ', width);
                    if (cut <= 0) cut = width;
                    lines.add(remaining.substring(0, cut));
                    remaining = remaining.substring(cut).stripLeading();
                }
                if (!remaining.isEmpty()) lines.add(remaining);
            }
        }
        return lines;
    }
}

