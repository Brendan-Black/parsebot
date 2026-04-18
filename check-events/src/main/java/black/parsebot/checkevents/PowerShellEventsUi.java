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

final class PowerShellEventsUi implements EventsUi {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void showEmpty(Path logDir) {
        String ps = PowerShellRunner.WINFORMS_PREAMBLE + """

                $form = New-Object System.Windows.Forms.Form
                $form.Text = 'ParseBot Events'
                $form.StartPosition = 'CenterScreen'
                $form.ClientSize = New-Object System.Drawing.Size(400,120)
                $form.FormBorderStyle = 'FixedDialog'
                $form.MaximizeBox = $false
                $form.TopMost = $true

                $lbl = New-Object System.Windows.Forms.Label
                $lbl.Location = New-Object System.Drawing.Point(10,20)
                $lbl.Size = New-Object System.Drawing.Size(380,40)
                $lbl.Text = 'No events found in: %s'
                $form.Controls.Add($lbl)

                $btnOk = New-Object System.Windows.Forms.Button
                $btnOk.Location = New-Object System.Drawing.Point(160,70)
                $btnOk.Size = New-Object System.Drawing.Size(80,30)
                $btnOk.Text = 'OK'
                $btnOk.DialogResult = [System.Windows.Forms.DialogResult]::OK
                $form.AcceptButton = $btnOk
                $form.Controls.Add($btnOk)

                [void]$form.ShowDialog()
                """.formatted(PowerShellRunner.escapeSingleLine(logDir.toString()));

        runPowerShell(ps);
    }

    @Override
    public void showEvents(List<Event> events, Path logDir) {
        StringBuilder ps = new StringBuilder();
        ps.append(PowerShellRunner.WINFORMS_PREAMBLE).append('\n');

        ps.append("$form = New-Object System.Windows.Forms.Form\n");
        ps.append("$form.Text = 'ParseBot Events (" + events.size() + " entries)'\n");
        ps.append("$form.StartPosition = 'CenterScreen'\n");
        ps.append("$form.ClientSize = New-Object System.Drawing.Size(900,500)\n");
        ps.append("$form.MinimumSize = New-Object System.Drawing.Size(700,300)\n");
        ps.append("$form.TopMost = $true\n\n");

        // DataGridView
        ps.append("$dgv = New-Object System.Windows.Forms.DataGridView\n");
        ps.append("$dgv.Location = New-Object System.Drawing.Point(10,10)\n");
        ps.append("$dgv.Size = New-Object System.Drawing.Size(875,440)\n");
        ps.append("$dgv.Anchor = 'Top,Bottom,Left,Right'\n");
        ps.append("$dgv.AllowUserToAddRows = $false\n");
        ps.append("$dgv.AllowUserToDeleteRows = $false\n");
        ps.append("$dgv.ReadOnly = $true\n");
        ps.append("$dgv.AutoSizeColumnsMode = 'Fill'\n");
        ps.append("$dgv.SelectionMode = 'FullRowSelect'\n");
        ps.append("$dgv.RowHeadersVisible = $false\n\n");

        // Columns
        ps.append("$dgv.Columns.Add('Timestamp', 'Timestamp') | Out-Null\n");
        ps.append("$dgv.Columns.Add('Type', 'Type') | Out-Null\n");
        ps.append("$dgv.Columns.Add('Severity', 'Severity') | Out-Null\n");
        ps.append("$dgv.Columns.Add('Message', 'Message') | Out-Null\n");
        ps.append("$dgv.Columns.Add('Details', 'Details') | Out-Null\n\n");

        // Column widths
        ps.append("$dgv.Columns['Timestamp'].FillWeight = 15\n");
        ps.append("$dgv.Columns['Type'].FillWeight = 15\n");
        ps.append("$dgv.Columns['Severity'].FillWeight = 10\n");
        ps.append("$dgv.Columns['Message'].FillWeight = 30\n");
        ps.append("$dgv.Columns['Details'].FillWeight = 30\n\n");

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

        ps.append("\n$form.Controls.Add($dgv)\n");

        // Status bar with log directory
        ps.append(String.format(
                "$status = New-Object System.Windows.Forms.StatusBar; " +
                "$status.Text = 'Logs: %s'; " +
                "$form.Controls.Add($status)\n",
                PowerShellRunner.escapeSingleLine(logDir.toString())));

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
