package black.parsebot.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String SERVICE_NAME = "ParseBot";
    private static final String DISPLAY_NAME = "ParseBot Service";
    private static final String DESCRIPTION = "ParseBot email parsing service";
    private static final String SERVICE_EXE = "service.exe";

    /** Property key → [label, default value] */
    private static final LinkedHashMap<String, String[]> FIELDS = new LinkedHashMap<>();
    static {
        FIELDS.put("poll.interval.seconds", new String[]{"Poll Interval (seconds)", "60"});
        FIELDS.put("input.directory",       new String[]{"Input Directory", "./input"});
        FIELDS.put("input.file.pattern",    new String[]{"Input File Pattern", "*.txt"});
        FIELDS.put("mail.host",             new String[]{"Mail Host", ""});
        FIELDS.put("mail.port",             new String[]{"Mail Port", "993"});
        FIELDS.put("mail.username",         new String[]{"Mail Username", ""});
        FIELDS.put("mail.password",         new String[]{"Mail Password", ""});
        FIELDS.put("mail.folder",           new String[]{"Mail Folder", "INBOX"});
        FIELDS.put("mail.folder.success",   new String[]{"Mail Success Folder", "Processed"});
        FIELDS.put("mail.folder.failed",    new String[]{"Mail Failed Folder", "Failed"});
        FIELDS.put("mail.protocol",         new String[]{"Mail Protocol", "imaps"});
        FIELDS.put("claude.api.key",        new String[]{"Claude API Key", ""});
        FIELDS.put("csv.customers",         new String[]{"Customers CSV Path", ""});
        FIELDS.put("csv.products",          new String[]{"Products CSV Path", ""});
        FIELDS.put("sftp.host",             new String[]{"SFTP Host", ""});
        FIELDS.put("sftp.port",             new String[]{"SFTP Port", "22"});
        FIELDS.put("sftp.username",         new String[]{"SFTP Username", ""});
        FIELDS.put("sftp.password",         new String[]{"SFTP Password", ""});
        FIELDS.put("sftp.private.key",      new String[]{"SFTP Private Key Path", ""});
        FIELDS.put("sftp.remote.directory", new String[]{"SFTP Remote Directory", "/upload"});
    }

    public static void main(String[] args) {
        List<String> argList = List.of(args);
        boolean dryRun = argList.contains("--dry-run");
        Path exePath = resolveExePath(args);

        String command = argList.stream()
                .filter(a -> !a.startsWith("--"))
                .findFirst()
                .orElse("install");

        switch (command) {
            case "install" -> install(exePath, dryRun);
            case "uninstall" -> uninstall();
            case "status" -> status();
            default -> {
                System.err.println("Unknown command: " + command);
                System.exit(1);
            }
        }
    }

    private static void install(Path exePath, boolean dryRun) {
        if (!dryRun && !Files.isRegularFile(exePath)) {
            System.err.println("Service executable not found: " + exePath);
            System.exit(1);
        }

        Map<String, String> values = showConfigGui();
        if (values == null) {
            System.out.println("Installation cancelled.");
            System.exit(0);
        }

        // Build binPath with -D flags so AppConfig picks them up as system properties
        StringBuilder binPath = new StringBuilder();
        binPath.append('"').append(exePath.toAbsolutePath()).append('"');
        for (var entry : values.entrySet()) {
            String val = entry.getValue();
            if (!val.isEmpty()) {
                binPath.append(" \"-Dparsebot.").append(entry.getKey()).append('=').append(val).append('"');
            }
        }

        System.out.println("binPath: " + binPath);

        if (dryRun) {
            System.out.println("[dry-run] Would run: sc create " + SERVICE_NAME + " binPath= " + binPath);
            return;
        }

        System.out.println("Installing " + SERVICE_NAME + "...");

        int rc = sc("create", SERVICE_NAME,
                "binPath=", binPath.toString(),
                "DisplayName=", DISPLAY_NAME,
                "start=", "auto");
        if (rc != 0) {
            System.err.println("Failed to create service (exit code " + rc + ").");
            System.exit(rc);
        }

        sc("description", SERVICE_NAME, DESCRIPTION);
        System.out.println(SERVICE_NAME + " installed successfully.");
        System.out.println("Start it with: sc start " + SERVICE_NAME);
    }

    private static void uninstall() {
        System.out.println("Stopping " + SERVICE_NAME + "...");
        sc("stop", SERVICE_NAME);
        System.out.println("Removing " + SERVICE_NAME + "...");
        int rc = sc("delete", SERVICE_NAME);
        if (rc != 0) {
            System.err.println("Failed to remove service (exit code " + rc + ").");
            System.exit(rc);
        }
        System.out.println(SERVICE_NAME + " uninstalled.");
    }

    private static void status() {
        sc("query", SERVICE_NAME);
    }

    /**
     * Launches a PowerShell WinForms dialog with fields for every config property.
     * Returns a map of property key → user-entered value, or null if cancelled.
     */
    private static Map<String, String> showConfigGui() {
        StringBuilder ps = new StringBuilder();
        ps.append("Add-Type -AssemblyName System.Windows.Forms\n");
        ps.append("Add-Type -AssemblyName System.Drawing\n");
        ps.append("[System.Windows.Forms.Application]::EnableVisualStyles()\n");

        ps.append("$form = New-Object System.Windows.Forms.Form\n");
        ps.append("$form.Text = 'ParseBot Service Configuration'\n");
        ps.append("$form.StartPosition = 'CenterScreen'\n");
        ps.append("$form.FormBorderStyle = 'FixedDialog'\n");
        ps.append("$form.MaximizeBox = $false\n");
        ps.append("$form.AutoScroll = $true\n");

        int y = 10;
        int fieldIndex = 0;
        List<String> keys = new ArrayList<>(FIELDS.keySet());

        for (var entry : FIELDS.entrySet()) {
            String label = entry.getValue()[0];
            String defaultVal = entry.getValue()[1];
            boolean isPassword = entry.getKey().contains("password") || entry.getKey().contains("api.key");

            ps.append(String.format(
                    "$lbl%d = New-Object System.Windows.Forms.Label; " +
                    "$lbl%d.Location = New-Object System.Drawing.Point(10,%d); " +
                    "$lbl%d.Size = New-Object System.Drawing.Size(200,20); " +
                    "$lbl%d.Text = '%s'; " +
                    "$form.Controls.Add($lbl%d)\n",
                    fieldIndex, fieldIndex, y, fieldIndex, fieldIndex, label, fieldIndex));

            ps.append(String.format(
                    "$txt%d = New-Object System.Windows.Forms.TextBox; " +
                    "$txt%d.Location = New-Object System.Drawing.Point(220,%d); " +
                    "$txt%d.Size = New-Object System.Drawing.Size(300,20); " +
                    "$txt%d.Text = '%s'",
                    fieldIndex, fieldIndex, y, fieldIndex, fieldIndex, defaultVal));
            if (isPassword) {
                ps.append(String.format("; $txt%d.UseSystemPasswordChar = $true", fieldIndex));
            }
            ps.append(String.format("; $form.Controls.Add($txt%d)\n", fieldIndex));

            y += 30;
            fieldIndex++;
        }

        // Buttons
        int buttonY = y + 10;
        ps.append(String.format(
                "$btnOk = New-Object System.Windows.Forms.Button; " +
                "$btnOk.Location = New-Object System.Drawing.Point(340,%d); " +
                "$btnOk.Size = New-Object System.Drawing.Size(80,30); " +
                "$btnOk.Text = 'Install'; " +
                "$btnOk.DialogResult = [System.Windows.Forms.DialogResult]::OK; " +
                "$form.AcceptButton = $btnOk; " +
                "$form.Controls.Add($btnOk)\n", buttonY));
        ps.append(String.format(
                "$btnCancel = New-Object System.Windows.Forms.Button; " +
                "$btnCancel.Location = New-Object System.Drawing.Point(440,%d); " +
                "$btnCancel.Size = New-Object System.Drawing.Size(80,30); " +
                "$btnCancel.Text = 'Cancel'; " +
                "$btnCancel.DialogResult = [System.Windows.Forms.DialogResult]::Cancel; " +
                "$form.CancelButton = $btnCancel; " +
                "$form.Controls.Add($btnCancel)\n", buttonY));

        int formHeight = buttonY + 80;
        ps.append(String.format("$form.ClientSize = New-Object System.Drawing.Size(540,%d)\n", formHeight));

        // Write results to a temp file so we don't depend on stdout piping
        try {
            Path resultFile = Files.createTempFile("parsebot-result-", ".txt");
            resultFile.toFile().deleteOnExit();
            String resultPath = resultFile.toString().replace("\\", "\\\\");

            ps.append("$form.TopMost = $true\n");
            ps.append("$result = $form.ShowDialog()\n");
            ps.append(String.format(
                    "if ($result -ne [System.Windows.Forms.DialogResult]::OK) { Set-Content -Path '%s' -Value 'CANCELLED'; exit }\n",
                    resultPath));

            ps.append(String.format("$out = @()\n"));
            for (int i = 0; i < keys.size(); i++) {
                ps.append(String.format("$out += '%s=' + $txt%d.Text\n", keys.get(i), i));
            }
            ps.append(String.format("Set-Content -Path '%s' -Value ($out -join \"`n\")\n", resultPath));

            Path scriptFile = Files.createTempFile("parsebot-installer-", ".ps1");
            Files.writeString(scriptFile, ps.toString());
            scriptFile.toFile().deleteOnExit();

            Process proc = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptFile.toString())
                    .inheritIO()
                    .start();

            int rc = proc.waitFor();

            List<String> lines = Files.readAllLines(resultFile);
            if (rc != 0 || lines.isEmpty() || "CANCELLED".equals(lines.getFirst())) {
                return null;
            }

            Map<String, String> values = new LinkedHashMap<>();
            for (String line : lines) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    values.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
            return values;

        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to launch configuration dialog: " + e.getMessage());
            return null;
        }
    }

    private static Path resolveExePath(String[] args) {
        for (int i = 1; i < args.length - 1; i++) {
            if ("--exe".equals(args[i])) {
                return Path.of(args[i + 1]).toAbsolutePath();
            }
        }
        return Path.of("").toAbsolutePath().resolve("service").resolve(SERVICE_EXE);
    }

    private static int sc(String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "sc.exe";
        System.arraycopy(args, 0, command, 1, args.length);
        try {
            return new ProcessBuilder(command).inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to run sc.exe: " + e.getMessage());
            return 1;
        }
    }
}
