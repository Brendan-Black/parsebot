package black.parsebot.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    private static final String SERVICE_NAME = "ParseBot";
    private static final String DISPLAY_NAME = "ParseBot Service";
    private static final String DESCRIPTION = "ParseBot email parsing service";
    private static final String SERVICE_EXE = "service.exe";

    private record Field(String key, String label, String defaultValue) {}

    private record Section(String title, List<Field> fields, String hint) {
        Section(String title, List<Field> fields) {
            this(title, fields, null);
        }
    }

    private static final List<Section> SECTIONS = List.of(
            new Section("General", List.of(
                    new Field("poll.interval.seconds", "Poll Interval (seconds)", "60")
            )),
            new Section("Mail (IMAP)", List.of(
                    new Field("mail.host",           "Host",           ""),
                    new Field("mail.port",           "Port",           "993"),
                    new Field("mail.username",       "Username",       ""),
                    new Field("mail.password",       "Password",       ""),
                    new Field("mail.folder",         "Folder",         "INBOX"),
                    new Field("mail.folder.success", "Success Folder", "Processed"),
                    new Field("mail.folder.failed",  "Failed Folder",  "Failed"),
                    new Field("mail.protocol",       "Protocol",       "imaps")
            )),
            new Section("Claude API", List.of(
                    new Field("claude.api.key", "API Key", "")
            )),
            new Section("Reference Data", List.of(
                    new Field("csv.customers",         "Customers CSV Path",              ""),
                    new Field("csv.products",          "Products CSV Path",               ""),
                    new Field("csv.pricematrix",       "Price Matrix CSV Path",           ""),
                    new Field("custom.rules.dir",      "Custom Rules Directory",          "custom_rules"),
                    new Field("custom.productlists.dir", "Custom Product Lists Directory", "custom_productlists")
            )),
            new Section("SFTP Output", List.of(
                    new Field("sftp.host",             "Host",             ""),
                    new Field("sftp.port",             "Port",             "22"),
                    new Field("sftp.username",         "Username",         ""),
                    new Field("sftp.password",         "Password",         ""),
                    new Field("sftp.private.key",      "Private Key Path", ""),
                    new Field("sftp.remote.directory", "Remote Directory", "/upload")
            )),
            new Section("Events", List.of(
                    new Field("events.consecutive.failure.threshold", "Consecutive Failure Threshold",   "3"),
                    new Field("events.report.schedule",               "Report Schedule (daily/weekly)",  "daily"),
                    new Field("events.report.time",                   "Report Time (HH:mm)",             "08:00")
            )),
            new Section("Notifications - Email (SMTP)", List.of(
                    new Field("notify.smtp.enabled",  "Enabled (true/false)", "false"),
                    new Field("notify.smtp.host",     "SMTP Host",            ""),
                    new Field("notify.smtp.port",     "SMTP Port",            "587"),
                    new Field("notify.smtp.to",       "To Addresses",         ""),
                    new Field("notify.smtp.to.urgent", "Urgent To Addresses", ""),
                    new Field("notify.smtp.starttls", "STARTTLS (true/false)","true")
            ),
                    "#@vtext.com (Verizon)\n" +
                    "#@tmomail.net (T-Mobile)\n" +
                    "#@txt.att.net (AT&T)\n" +
                    "#@messaging.sprintpcs.com (Sprint)\n" +
                    "#@msg.fi.google.com (Google Fi)\n" +
                    "#@message.ting.com (Ting)\n" +
                    "#@email.uscc.net (US Cellular)\n" +
                    "#@sms.cricketwireless.net (Cricket)\n" +
                    "#@myboostmobile.com (Boost)\n" +
                    "#@text.republicwireless.com (Republic)\n" +
                    "#@vmobl.com (Virgin Mobile)\n" +
                    "#@mmst5.tracfone.com (Tracfone)\n" +
                    "#@mymetropcs.com (Metro)\n" +
                    "#@sms.mypage.com (PagePlus)\n" +
                    "#@mailmymobile.net (Consumer Cellular)\n" +
                    "#@cspire1.com (C-Spire)"
            ),
            new Section("Notifications - Teams", List.of(
                    new Field("notify.teams.enabled",     "Enabled (true/false)", "false"),
                    new Field("notify.teams.webhook.url", "Webhook URL",          "")
            ))
    );

    public static void main(String[] args) {
        List<String> argList = List.of(args);
        boolean dryRun = argList.contains("--dry-run");
        Path exePath = resolveExePath(args);

        String command = argList.stream()
                .filter(a -> !a.startsWith("--"))
                .findFirst()
                .orElse(null);

        if (command == null) {
            // No command given (e.g. double-click) — show launcher dialog
            command = showLauncherGui();
            if (command == null) {
                System.out.println("Cancelled.");
                System.exit(0);
            }
        }

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
        sc("failure", SERVICE_NAME, "reset=", "86400", "actions=", "restart/5000/restart/10000/restart/30000");
        System.out.println(SERVICE_NAME + " installed successfully. Starting...");

        int startRc = sc("start", SERVICE_NAME);
        if (startRc != 0) {
            System.err.println("Service installed but failed to start (exit code " + startRc + ").");
            System.err.println("You can start it manually: sc start " + SERVICE_NAME);
        } else {
            System.out.println(SERVICE_NAME + " is running.");
        }
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
     * Launches a PowerShell WinForms dialog asking the user to Install or Uninstall.
     * Returns "install", "uninstall", or null if cancelled/closed.
     */
    private static String showLauncherGui() {
        try {
            Path resultFile = Files.createTempFile("parsebot-launcher-", ".txt");
            resultFile.toFile().deleteOnExit();
            String resultPath = resultFile.toString().replace("\\", "\\\\");

            String ps = """
                    Add-Type -AssemblyName System.Windows.Forms
                    Add-Type -AssemblyName System.Drawing
                    [System.Windows.Forms.Application]::EnableVisualStyles()

                    $form = New-Object System.Windows.Forms.Form
                    $form.Text = 'ParseBot Installer'
                    $form.StartPosition = 'CenterScreen'
                    $form.FormBorderStyle = 'FixedDialog'
                    $form.MaximizeBox = $false
                    $form.ClientSize = New-Object System.Drawing.Size(320,130)
                    $form.TopMost = $true

                    $lbl = New-Object System.Windows.Forms.Label
                    $lbl.Location = New-Object System.Drawing.Point(10,15)
                    $lbl.Size = New-Object System.Drawing.Size(300,20)
                    $lbl.Text = 'What would you like to do?'
                    $form.Controls.Add($lbl)

                    $btnInstall = New-Object System.Windows.Forms.Button
                    $btnInstall.Location = New-Object System.Drawing.Point(20,50)
                    $btnInstall.Size = New-Object System.Drawing.Size(130,40)
                    $btnInstall.Text = 'Install'
                    $form.Controls.Add($btnInstall)

                    $btnUninstall = New-Object System.Windows.Forms.Button
                    $btnUninstall.Location = New-Object System.Drawing.Point(170,50)
                    $btnUninstall.Size = New-Object System.Drawing.Size(130,40)
                    $btnUninstall.Text = 'Uninstall'
                    $form.Controls.Add($btnUninstall)

                    $btnInstall.Add_Click({
                        Set-Content -Path '%s' -Value 'install'
                        $form.Close()
                    })
                    $btnUninstall.Add_Click({
                        Set-Content -Path '%s' -Value 'uninstall'
                        $form.Close()
                    })

                    $form.Add_FormClosed({
                        if (-not (Test-Path '%s') -or (Get-Content '%s') -eq '') {
                            Set-Content -Path '%s' -Value 'CANCELLED'
                        }
                    })

                    [void]$form.ShowDialog()
                    """.formatted(resultPath, resultPath, resultPath, resultPath, resultPath);

            Path scriptFile = Files.createTempFile("parsebot-launcher-", ".ps1");
            Files.writeString(scriptFile, ps);
            scriptFile.toFile().deleteOnExit();

            Process proc = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptFile.toString())
                    .inheritIO()
                    .start();
            proc.waitFor();

            List<String> lines = Files.readAllLines(resultFile);
            if (lines.isEmpty() || "CANCELLED".equals(lines.getFirst())) {
                return null;
            }
            return lines.getFirst().trim();

        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to launch dialog: " + e.getMessage());
            return null;
        }
    }

    /**
     * Launches a PowerShell WinForms dialog with fields for every config property.
     * Returns a map of property key → user-entered value, or null if cancelled.
     */
    private static Map<String, String> showConfigGui() {
        StringBuilder ps = new StringBuilder();
        ps.append("Add-Type -AssemblyName System.Windows.Forms\n");
        ps.append("Add-Type -AssemblyName System.Drawing\n");
        ps.append("[System.Windows.Forms.Application]::EnableVisualStyles()\n\n");

        ps.append("$form = New-Object System.Windows.Forms.Form\n");
        ps.append("$form.Text = 'ParseBot Service Configuration'\n");
        ps.append("$form.StartPosition = 'CenterScreen'\n");
        ps.append("$form.FormBorderStyle = 'FixedDialog'\n");
        ps.append("$form.MaximizeBox = $false\n");
        ps.append("$form.ClientSize = New-Object System.Drawing.Size(560,400)\n\n");

        // TabControl fills most of the form, buttons sit below
        ps.append("$tabs = New-Object System.Windows.Forms.TabControl\n");
        ps.append("$tabs.Location = New-Object System.Drawing.Point(10,10)\n");
        ps.append("$tabs.Size = New-Object System.Drawing.Size(535,340)\n");
        ps.append("$tabs.Multiline = $true\n");
        ps.append("$tabs.Appearance = [System.Windows.Forms.TabAppearance]::FlatButtons\n");
        ps.append("$form.Controls.Add($tabs)\n\n");

        Set<String> folderFields = Set.of("custom.rules.dir", "custom.productlists.dir");
        Set<String> fileFields = Set.of("csv.customers", "csv.products", "csv.pricematrix", "sftp.private.key");
        Set<String> booleanFields = Set.of("notify.smtp.enabled", "notify.smtp.starttls", "notify.teams.enabled");
        Set<String> multilineFields = Set.of("notify.smtp.to", "notify.smtp.to.urgent");

        int fieldIndex = 0;
        List<String> keys = new ArrayList<>();
        List<Boolean> isBooleanField = new ArrayList<>();

        for (int s = 0; s < SECTIONS.size(); s++) {
            Section section = SECTIONS.get(s);

            ps.append(String.format("$tab%d = New-Object System.Windows.Forms.TabPage\n", s));
            ps.append(String.format("$tab%d.Text = '%s'\n", s, section.title()));
            ps.append(String.format("$tab%d.AutoScroll = $true\n", s));
            ps.append(String.format("$tabs.TabPages.Add($tab%d)\n\n", s));

            int y = 15;
            if (section.hint() != null) {
                // Collapsible toggle link
                ps.append(String.format(
                        "$hintToggle%d = New-Object System.Windows.Forms.LinkLabel; " +
                        "$hintToggle%d.Location = New-Object System.Drawing.Point(10,%d); " +
                        "$hintToggle%d.Size = New-Object System.Drawing.Size(490,16); " +
                        "$hintToggle%d.Text = '+ SMS carrier gateway reference'; " +
                        "$tab%d.Controls.Add($hintToggle%d)\n",
                        s, s, y, s, s, s, s));
                y += 20;
                // Detail panel with border (hidden by default)
                ps.append(String.format(
                        "$hintPanel%d = New-Object System.Windows.Forms.Panel; " +
                        "$hintPanel%d.Location = New-Object System.Drawing.Point(10,%d); " +
                        "$hintPanel%d.Size = New-Object System.Drawing.Size(495,230); " +
                        "$hintPanel%d.BorderStyle = [System.Windows.Forms.BorderStyle]::FixedSingle; " +
                        "$hintPanel%d.BackColor = [System.Drawing.Color]::FromArgb(245,245,250); " +
                        "$hintPanel%d.Visible = $false; " +
                        "$tab%d.Controls.Add($hintPanel%d)\n",
                        s, s, y, s, s, s, s, s, s));
                // DataGridView as a read-only table inside the panel
                ps.append(String.format(
                        "$hintGrid%d = New-Object System.Windows.Forms.DataGridView; " +
                        "$hintGrid%d.Location = New-Object System.Drawing.Point(0,0); " +
                        "$hintGrid%d.Size = New-Object System.Drawing.Size(493,228); " +
                        "$hintGrid%d.Anchor = 'Top,Bottom,Left,Right'; " +
                        "$hintGrid%d.ReadOnly = $true; " +
                        "$hintGrid%d.AllowUserToAddRows = $false; " +
                        "$hintGrid%d.AllowUserToDeleteRows = $false; " +
                        "$hintGrid%d.AllowUserToResizeRows = $false; " +
                        "$hintGrid%d.RowHeadersVisible = $false; " +
                        "$hintGrid%d.SelectionMode = 'FullRowSelect'; " +
                        "$hintGrid%d.BackgroundColor = [System.Drawing.Color]::FromArgb(245,245,250); " +
                        "$hintGrid%d.BorderStyle = 'None'; " +
                        "$hintGrid%d.AutoSizeColumnsMode = 'Fill'; " +
                        "$hintGrid%d.ColumnHeadersDefaultCellStyle.BackColor = [System.Drawing.Color]::FromArgb(230,230,235); " +
                        "$hintGrid%d.EnableHeadersVisualStyles = $false; " +
                        "$hintGrid%d.Columns.Add('Gateway', 'Gateway Address') | Out-Null; " +
                        "$hintGrid%d.Columns.Add('Carrier', 'Carrier') | Out-Null; " +
                        "$hintGrid%d.Columns['Gateway'].FillWeight = 55; " +
                        "$hintGrid%d.Columns['Carrier'].FillWeight = 45; " +
                        "$hintPanel%d.Controls.Add($hintGrid%d)\n",
                        s, s, s, s, s, s, s, s, s, s, s, s, s, s, s, s, s, s, s, s, s));
                // Add rows
                String[][] carriers = {
                        {"#@vtext.com", "Verizon"},
                        {"#@tmomail.net", "T-Mobile"},
                        {"#@txt.att.net", "AT&T"},
                        {"#@messaging.sprintpcs.com", "Sprint"},
                        {"#@msg.fi.google.com", "Google Fi"},
                        {"#@message.ting.com", "Ting"},
                        {"#@email.uscc.net", "US Cellular"},
                        {"#@sms.cricketwireless.net", "Cricket"},
                        {"#@myboostmobile.com", "Boost"},
                        {"#@text.republicwireless.com", "Republic"},
                        {"#@vmobl.com", "Virgin Mobile"},
                        {"#@mmst5.tracfone.com", "Tracfone"},
                        {"#@mymetropcs.com", "Metro"},
                        {"#@sms.mypage.com", "PagePlus"},
                        {"#@mailmymobile.net", "Consumer Cellular"},
                        {"#@cspire1.com", "C-Spire"},
                };
                for (String[] row : carriers) {
                    ps.append(String.format("$hintGrid%d.Rows.Add('%s', '%s') | Out-Null\n", s, row[0], row[1]));
                }
                ps.append(String.format("$hintGrid%d.ClearSelection()\n", s));
                // Toggle click handler
                ps.append(String.format(
                        "$hintToggle%d.Add_LinkClicked({ " +
                        "if ($hintPanel%d.Visible) { " +
                        "$hintPanel%d.Visible = $false; " +
                        "$hintToggle%d.Text = '+ SMS carrier gateway reference' " +
                        "} else { " +
                        "$hintPanel%d.Visible = $true; " +
                        "$hintToggle%d.Text = '- SMS carrier gateway reference' " +
                        "} })\n",
                        s, s, s, s, s, s));
                y += 5; // small gap; detail panel overlaps when expanded since tab has AutoScroll
            }
            for (Field field : section.fields()) {
                String key = field.key();
                String label = field.label();
                String defaultVal = field.defaultValue();
                boolean isPassword = key.contains("password") || key.contains("api.key");
                boolean hasBrowse = folderFields.contains(key) || fileFields.contains(key);
                boolean isBoolean = booleanFields.contains(key);
                boolean isMultiline = multilineFields.contains(key);
                keys.add(key);
                isBooleanField.add(isBoolean);

                // Strip "(true/false)" hint from label for boolean fields
                String displayLabel = isBoolean ? label.replace(" (true/false)", "") : label;

                ps.append(String.format(
                        "$lbl%d = New-Object System.Windows.Forms.Label; " +
                        "$lbl%d.Location = New-Object System.Drawing.Point(10,%d); " +
                        "$lbl%d.Size = New-Object System.Drawing.Size(190,20); " +
                        "$lbl%d.Text = '%s'; " +
                        "$tab%d.Controls.Add($lbl%d)\n",
                        fieldIndex, fieldIndex, y, fieldIndex, fieldIndex, displayLabel, s, fieldIndex));

                if (isBoolean) {
                    boolean defaultTrue = "true".equals(defaultVal);
                    // Panel to isolate this radio button group
                    ps.append(String.format(
                            "$pnl%d = New-Object System.Windows.Forms.Panel; " +
                            "$pnl%d.Location = New-Object System.Drawing.Point(210,%d); " +
                            "$pnl%d.Size = New-Object System.Drawing.Size(140,22); " +
                            "$tab%d.Controls.Add($pnl%d)\n",
                            fieldIndex, fieldIndex, y, fieldIndex, s, fieldIndex));
                    // "Yes" radio
                    ps.append(String.format(
                            "$radYes%d = New-Object System.Windows.Forms.RadioButton; " +
                            "$radYes%d.Location = New-Object System.Drawing.Point(0,0); " +
                            "$radYes%d.Size = New-Object System.Drawing.Size(60,20); " +
                            "$radYes%d.Text = 'Yes'; " +
                            "$radYes%d.Checked = %s; " +
                            "$pnl%d.Controls.Add($radYes%d)\n",
                            fieldIndex, fieldIndex, fieldIndex, fieldIndex,
                            fieldIndex, defaultTrue ? "$true" : "$false", fieldIndex, fieldIndex));
                    // "No" radio
                    ps.append(String.format(
                            "$radNo%d = New-Object System.Windows.Forms.RadioButton; " +
                            "$radNo%d.Location = New-Object System.Drawing.Point(70,0); " +
                            "$radNo%d.Size = New-Object System.Drawing.Size(60,20); " +
                            "$radNo%d.Text = 'No'; " +
                            "$radNo%d.Checked = %s; " +
                            "$pnl%d.Controls.Add($radNo%d)\n",
                            fieldIndex, fieldIndex, fieldIndex, fieldIndex,
                            fieldIndex, defaultTrue ? "$false" : "$true", fieldIndex, fieldIndex));
                } else if (isMultiline) {
                    ps.append(String.format(
                            "$txt%d = New-Object System.Windows.Forms.TextBox; " +
                            "$txt%d.Location = New-Object System.Drawing.Point(210,%d); " +
                            "$txt%d.Size = New-Object System.Drawing.Size(300,60); " +
                            "$txt%d.Multiline = $true; " +
                            "$txt%d.ScrollBars = 'Vertical'; " +
                            "$txt%d.AcceptsReturn = $true; " +
                            "$txt%d.Text = '%s'; " +
                            "$tab%d.Controls.Add($txt%d)\n",
                            fieldIndex, fieldIndex, y, fieldIndex, fieldIndex,
                            fieldIndex, fieldIndex, fieldIndex, defaultVal, s, fieldIndex));
                    y += 40; // extra height for multiline
                } else {
                    int textBoxWidth = hasBrowse ? 230 : 300;
                    ps.append(String.format(
                            "$txt%d = New-Object System.Windows.Forms.TextBox; " +
                            "$txt%d.Location = New-Object System.Drawing.Point(210,%d); " +
                            "$txt%d.Size = New-Object System.Drawing.Size(%d,20); " +
                            "$txt%d.Text = '%s'",
                            fieldIndex, fieldIndex, y, fieldIndex, textBoxWidth, fieldIndex, defaultVal));
                    if (isPassword) {
                        ps.append(String.format("; $txt%d.UseSystemPasswordChar = $true", fieldIndex));
                    }
                    ps.append(String.format("; $tab%d.Controls.Add($txt%d)\n", s, fieldIndex));

                    if (hasBrowse) {
                        ps.append(String.format(
                                "$btn%d = New-Object System.Windows.Forms.Button; " +
                                "$btn%d.Location = New-Object System.Drawing.Point(445,%d); " +
                                "$btn%d.Size = New-Object System.Drawing.Size(65,22); " +
                                "$btn%d.Text = 'Browse'; " +
                                "$tab%d.Controls.Add($btn%d)\n",
                                fieldIndex, fieldIndex, y, fieldIndex, fieldIndex, s, fieldIndex));

                        if (folderFields.contains(key)) {
                            ps.append(String.format(
                                    "$btn%d.Add_Click({ " +
                                    "$dlg = New-Object System.Windows.Forms.FolderBrowserDialog; " +
                                    "$dlg.Description = '%s'; " +
                                    "$dlg.ShowNewFolderButton = $true; " +
                                    "if ($dlg.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { " +
                                    "$txt%d.Text = $dlg.SelectedPath } })\n",
                                    fieldIndex, label, fieldIndex));
                        } else {
                            ps.append(String.format(
                                    "$btn%d.Add_Click({ " +
                                    "$dlg = New-Object System.Windows.Forms.OpenFileDialog; " +
                                    "$dlg.Title = '%s'; " +
                                    "$dlg.Filter = 'All Files (*.*)|*.*'; " +
                                    "if ($dlg.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { " +
                                    "$txt%d.Text = $dlg.FileName } })\n",
                                    fieldIndex, label, fieldIndex));
                        }
                    }
                }

                y += 30;
                fieldIndex++;
            }
        }

        // Install / Cancel buttons below the tab control
        ps.append("\n$btnOk = New-Object System.Windows.Forms.Button; " +
                "$btnOk.Location = New-Object System.Drawing.Point(360,358); " +
                "$btnOk.Size = New-Object System.Drawing.Size(80,30); " +
                "$btnOk.Text = 'Install'; " +
                "$btnOk.DialogResult = [System.Windows.Forms.DialogResult]::OK; " +
                "$form.AcceptButton = $btnOk; " +
                "$form.Controls.Add($btnOk)\n");
        ps.append("$btnCancel = New-Object System.Windows.Forms.Button; " +
                "$btnCancel.Location = New-Object System.Drawing.Point(455,358); " +
                "$btnCancel.Size = New-Object System.Drawing.Size(80,30); " +
                "$btnCancel.Text = 'Cancel'; " +
                "$btnCancel.DialogResult = [System.Windows.Forms.DialogResult]::Cancel; " +
                "$form.CancelButton = $btnCancel; " +
                "$form.Controls.Add($btnCancel)\n");

        // Write results to a temp file so we don't depend on stdout piping
        try {
            Path resultFile = Files.createTempFile("parsebot-result-", ".txt");
            resultFile.toFile().deleteOnExit();
            String resultPath = resultFile.toString().replace("\\", "\\\\");

            ps.append("\n$form.TopMost = $true\n");
            ps.append("$result = $form.ShowDialog()\n");
            ps.append(String.format(
                    "if ($result -ne [System.Windows.Forms.DialogResult]::OK) { Set-Content -Path '%s' -Value 'CANCELLED'; exit }\n",
                    resultPath));

            ps.append("$out = @()\n");
            for (int i = 0; i < keys.size(); i++) {
                if (isBooleanField.get(i)) {
                    ps.append(String.format(
                            "$out += '%s=' + $(if ($radYes%d.Checked) { 'true' } else { 'false' })\n",
                            keys.get(i), i));
                } else if (multilineFields.contains(keys.get(i))) {
                    // Join newline-separated entries with commas for property storage
                    ps.append(String.format(
                            "$out += '%s=' + (($txt%d.Text -split \"`r?`n\" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }) -join ',')\n",
                            keys.get(i), i));
                } else {
                    ps.append(String.format("$out += '%s=' + $txt%d.Text\n", keys.get(i), i));
                }
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
        // Default: service.exe sits next to the installer in the same directory
        Path installerDir = Path.of(ProcessHandle.current().info().command().orElse(""))
                .toAbsolutePath().getParent();
        if (installerDir != null && Files.isRegularFile(installerDir.resolve(SERVICE_EXE))) {
            return installerDir.resolve(SERVICE_EXE);
        }
        // Fallback: look in current working directory
        return Path.of("").toAbsolutePath().resolve(SERVICE_EXE);
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
