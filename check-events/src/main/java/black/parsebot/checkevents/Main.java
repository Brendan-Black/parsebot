package black.parsebot.checkevents;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

public class Main {

    private static final String EVENT_MARKER = "EVENT: ";

    private record Event(
            String id,
            String type,
            String severity,
            Instant timestamp,
            String message,
            Map<String, String> details
    ) {}

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void main(String[] args) {
        Path logDir = resolveLogDir(args);

        if (!Files.isDirectory(logDir)) {
            System.err.println("Log directory not found: " + logDir);
            System.err.println("Usage: check-events [--logs <directory>]");
            System.exit(1);
        }

        List<Event> events;
        try {
            events = parseEventsFromLogs(logDir);
        } catch (IOException e) {
            System.err.println("Failed to read log files: " + e.getMessage());
            System.exit(1);
            return;
        }

        if (events.isEmpty()) {
            showEmptyGui(logDir);
        } else {
            showEventsGui(events, logDir);
        }
    }

    private static Path resolveLogDir(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--logs".equals(args[i])) {
                return Path.of(args[i + 1]).toAbsolutePath();
            }
        }
        return Path.of("logs").toAbsolutePath();
    }

    private static List<Event> parseEventsFromLogs(Path logDir) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, type, ctx) ->
                        Instant.parse(json.getAsString()))
                .create();

        // Collect all parsebot log files (current + rotated)
        List<Path> logFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "parsebot*.log")) {
            for (Path path : stream) {
                logFiles.add(path);
            }
        }
        // Sort by filename so rotated logs appear in chronological order
        logFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));

        List<Event> events = new ArrayList<>();
        for (Path logFile : logFiles) {
            for (String line : Files.readAllLines(logFile)) {
                int idx = line.indexOf(EVENT_MARKER);
                if (idx < 0) continue;

                String json = line.substring(idx + EVENT_MARKER.length());
                try {
                    Event event = gson.fromJson(json, Event.class);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    // Skip malformed event lines
                }
            }
        }
        return events;
    }

    private static void showEmptyGui(Path logDir) {
        String ps = """
                Add-Type -AssemblyName System.Windows.Forms
                Add-Type -AssemblyName System.Drawing
                [System.Windows.Forms.Application]::EnableVisualStyles()

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
                """.formatted(escapePsString(logDir.toString()));

        runPowerShell(ps);
    }

    private static void showEventsGui(List<Event> events, Path logDir) {
        StringBuilder ps = new StringBuilder();
        ps.append("Add-Type -AssemblyName System.Windows.Forms\n");
        ps.append("Add-Type -AssemblyName System.Drawing\n");
        ps.append("[System.Windows.Forms.Application]::EnableVisualStyles()\n\n");

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
            String type = event.type() != null ? event.type() : "N/A";
            String severity = event.severity() != null ? event.severity() : "N/A";
            String message = event.message() != null ? event.message() : "";
            String details = formatDetails(event.details());

            ps.append(String.format(
                    "$idx = $dgv.Rows.Add('%s', '%s', '%s', '%s', '%s')\n",
                    escapePsString(timestamp),
                    escapePsString(type),
                    escapePsString(severity),
                    escapePsString(message),
                    escapePsString(details)));

            if ("CRITICAL".equals(severity)) {
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
                escapePsString(logDir.toString())));

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

    private static String escapePsString(String input) {
        return input.replace("'", "''").replace("\n", " ").replace("\r", "");
    }

    private static void runPowerShell(String script) {
        try {
            Path scriptFile = Files.createTempFile("parsebot-events-", ".ps1");
            Files.writeString(scriptFile, script);
            scriptFile.toFile().deleteOnExit();

            Process proc = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptFile.toString())
                    .inheritIO()
                    .start();
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to launch events viewer: " + e.getMessage());
        }
    }
}
