package black.parsebot.checkevents;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import black.parsebot.event.Event;
import black.parsebot.event.EventSeverity;
import black.parsebot.ps.PowerShellRunner;
import black.parsebot.ps.WinFormsScript;

final class PowerShellEventsUi implements EventsUi {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void showEmpty(Path logDir) {
        StringBuilder ps = new StringBuilder();
        ps.append(PowerShellRunner.WINFORMS_PREAMBLE).append('\n');
        WinFormsScript.form(ps, "form", "ParseBot Events", 400, 120, true, true);
        WinFormsScript.label(ps, "lbl", "form", 10, 20, 380, 40, "No events found in: " + logDir);
        WinFormsScript.button(ps, "btnOk", "form", 160, 70, 80, 30, "OK", "OK");
        ps.append("$form.AcceptButton = $btnOk\n");
        ps.append("[void]$form.ShowDialog()\n");

        runPowerShell(ps.toString());
    }

    @Override
    public void showEvents(List<Event> events, Path logDir) {
        StringBuilder ps = new StringBuilder();
        ps.append(PowerShellRunner.WINFORMS_PREAMBLE).append('\n');

        WinFormsScript.form(ps, "form", "ParseBot Events (" + events.size() + " entries)",
                900, 500, false, true);
        ps.append("$form.MinimumSize = New-Object System.Drawing.Size(700,300)\n\n");

        WinFormsScript.dataGridView(ps, "dgv", "form", 10, 10, 875, 440,
                List.of("Timestamp", "Type", "Severity", "Message", "Details"),
                List.of(15, 15, 10, 30, 30));
        ps.append("$dgv.Anchor = 'Top,Bottom,Left,Right'\n\n");

        // Add rows (newest first)
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            String timestamp = event.timestamp() != null ? DISPLAY_FORMAT.format(event.timestamp()) : "N/A";
            String type = event.type() != null ? event.type().name() : "N/A";
            String severity = event.severity() != null ? event.severity().name() : "N/A";
            String message = event.message() != null ? event.message() : "";
            String details = formatDetails(event.details());

            ps.append(String.format(
                    "$idx = $dgv.Rows.Add('%s', '%s', '%s', '%s', '%s')\n",
                    PowerShellRunner.escapeSingleLine(timestamp),
                    PowerShellRunner.escapeSingleLine(type),
                    PowerShellRunner.escapeSingleLine(severity),
                    PowerShellRunner.escapeSingleLine(message),
                    PowerShellRunner.escapeSingleLine(details)));

            if (event.severity() == EventSeverity.CRITICAL) {
                ps.append("$dgv.Rows[$idx].DefaultCellStyle.BackColor = [System.Drawing.Color]::MistyRose\n");
                ps.append("$dgv.Rows[$idx].DefaultCellStyle.ForeColor = [System.Drawing.Color]::DarkRed\n");
            }
        }

        ps.append('\n');
        WinFormsScript.statusBar(ps, "status", "form", "Logs: " + logDir);

        ps.append("\n[void]$form.ShowDialog()\n");

        runPowerShell(ps.toString());
    }

    private static String formatDetails(Map<String, String> details) {
        if (details == null || details.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        details.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(k).append("=").append(v);
        });
        return sb.toString();
    }

    private static void runPowerShell(String script) {
        try {
            PowerShellRunner.run(script, "parsebot-events-");
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to launch events viewer: " + e.getMessage());
        }
    }
}
