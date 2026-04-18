package black.parsebot.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import black.parsebot.ps.PowerShellRunner;

import static black.parsebot.installer.InstallerConfig.*;

final class PowerShellInstallerUi implements InstallerUi {

    @Override
    public String showLauncherDialog() {
        try {
            Path resultFile = Files.createTempFile("parsebot-launcher-", ".txt");
            resultFile.toFile().deleteOnExit();
            String resultPath = resultFile.toString().replace("\\", "\\\\");

            String ps = PowerShellRunner.WINFORMS_PREAMBLE + """

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

            PowerShellRunner.run(ps, "parsebot-launcher-");

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

    @Override
    public Map<String, String> showConfigDialog(Map<String, String> existing) {
        StringBuilder ps = new StringBuilder();
        ps.append(PowerShellRunner.WINFORMS_PREAMBLE).append('\n');

        appendFormSetup(ps);

        int fieldIndex = 0;
        List<String> keys = new ArrayList<>();
        List<Boolean> isBooleanField = new ArrayList<>();
        List<Boolean> isTimeField = new ArrayList<>();

        for (int s = 0; s < SECTIONS.size(); s++) {
            Section section = SECTIONS.get(s);
            appendTabPage(ps, s, section.title());

            int y = 15;
            if (section.hint() != null) {
                y = appendSmsHintPanel(ps, s, y);
            }

            for (Field field : section.fields()) {
                String key = field.key();
                String defaultVal = existing.getOrDefault(key, field.defaultValue());
                boolean isBoolean = BOOLEAN_FIELDS.contains(key);
                boolean isTime = TIME_FIELDS.contains(key);

                keys.add(key);
                isBooleanField.add(isBoolean);
                isTimeField.add(isTime);

                appendLabel(ps, fieldIndex, s, y, field.label(), isBoolean);

                if (isBoolean) {
                    appendBooleanRadios(ps, fieldIndex, s, y, defaultVal);
                } else if (isTime) {
                    defaultVal = PowerShellRunner.escapeSingleQuote(defaultVal);
                    appendTimePicker(ps, fieldIndex, s, y, defaultVal);
                } else if (MULTILINE_FIELDS.contains(key)) {
                    defaultVal = escapeForMultiline(defaultVal);
                    appendMultilineTextBox(ps, fieldIndex, s, y, defaultVal);
                    y += 40;
                } else {
                    defaultVal = PowerShellRunner.escapeSingleQuote(defaultVal);
                    boolean hasBrowse = FOLDER_FIELDS.contains(key) || FILE_FIELDS.contains(key);
                    boolean isPassword = key.contains("password") || key.contains("api.key");
                    appendTextBox(ps, fieldIndex, s, y, defaultVal, hasBrowse, isPassword);
                    if (hasBrowse) {
                        appendBrowseButton(ps, fieldIndex, s, y, field.label(), FOLDER_FIELDS.contains(key));
                    }
                }

                y += 30;
                fieldIndex++;
            }
        }

        appendFormButtons(ps);

        try {
            Path resultFile = Files.createTempFile("parsebot-result-", ".txt");
            resultFile.toFile().deleteOnExit();
            String resultPath = resultFile.toString().replace("\\", "\\\\");

            appendResultCollection(ps, resultPath, keys, isBooleanField, isTimeField);

            String raw = PowerShellRunner.runWithResult(ps.toString(), "parsebot-installer-", resultFile);
            if (raw == null) return null;

            Map<String, String> values = new LinkedHashMap<>();
            for (String line : raw.split("\n")) {
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

    // --- Form structure ---

    private static void appendFormSetup(StringBuilder ps) {
        ps.append("$form = New-Object System.Windows.Forms.Form\n");
        ps.append("$form.Text = 'ParseBot Service Configuration'\n");
        ps.append("$form.StartPosition = 'CenterScreen'\n");
        ps.append("$form.FormBorderStyle = 'FixedDialog'\n");
        ps.append("$form.MaximizeBox = $false\n");
        ps.append("$form.ClientSize = New-Object System.Drawing.Size(560,400)\n\n");

        ps.append("$tabs = New-Object System.Windows.Forms.TabControl\n");
        ps.append("$tabs.Location = New-Object System.Drawing.Point(10,10)\n");
        ps.append("$tabs.Size = New-Object System.Drawing.Size(535,340)\n");
        ps.append("$tabs.Multiline = $true\n");
        ps.append("$tabs.Appearance = [System.Windows.Forms.TabAppearance]::FlatButtons\n");
        ps.append("$form.Controls.Add($tabs)\n\n");
    }

    private static void appendTabPage(StringBuilder ps, int tabIndex, String title) {
        ps.append(String.format("$tab%d = New-Object System.Windows.Forms.TabPage\n", tabIndex));
        ps.append(String.format("$tab%d.Text = '%s'\n", tabIndex, title));
        ps.append(String.format("$tab%d.AutoScroll = $true\n", tabIndex));
        ps.append(String.format("$tabs.TabPages.Add($tab%d)\n\n", tabIndex));
    }

    private static void appendFormButtons(StringBuilder ps) {
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
    }

    // --- Field controls ---

    private static void appendLabel(StringBuilder ps, int idx, int tabIdx, int y, String label, boolean isBoolean) {
        String displayLabel = isBoolean ? label.replace(" (true/false)", "") : label;
        ps.append(String.format(
                "$lbl%d = New-Object System.Windows.Forms.Label; " +
                "$lbl%d.Location = New-Object System.Drawing.Point(10,%d); " +
                "$lbl%d.Size = New-Object System.Drawing.Size(190,20); " +
                "$lbl%d.Text = '%s'; " +
                "$tab%d.Controls.Add($lbl%d)\n",
                idx, idx, y, idx, idx, displayLabel, tabIdx, idx));
    }

    private static void appendBooleanRadios(StringBuilder ps, int idx, int tabIdx, int y, String defaultVal) {
        boolean defaultTrue = "true".equals(defaultVal);
        ps.append(String.format(
                "$pnl%d = New-Object System.Windows.Forms.Panel; " +
                "$pnl%d.Location = New-Object System.Drawing.Point(210,%d); " +
                "$pnl%d.Size = New-Object System.Drawing.Size(140,22); " +
                "$tab%d.Controls.Add($pnl%d)\n",
                idx, idx, y, idx, tabIdx, idx));
        ps.append(String.format(
                "$radYes%d = New-Object System.Windows.Forms.RadioButton; " +
                "$radYes%d.Location = New-Object System.Drawing.Point(0,0); " +
                "$radYes%d.Size = New-Object System.Drawing.Size(60,20); " +
                "$radYes%d.Text = 'Yes'; " +
                "$radYes%d.Checked = %s; " +
                "$pnl%d.Controls.Add($radYes%d)\n",
                idx, idx, idx, idx, idx, defaultTrue ? "$true" : "$false", idx, idx));
        ps.append(String.format(
                "$radNo%d = New-Object System.Windows.Forms.RadioButton; " +
                "$radNo%d.Location = New-Object System.Drawing.Point(70,0); " +
                "$radNo%d.Size = New-Object System.Drawing.Size(60,20); " +
                "$radNo%d.Text = 'No'; " +
                "$radNo%d.Checked = %s; " +
                "$pnl%d.Controls.Add($radNo%d)\n",
                idx, idx, idx, idx, idx, defaultTrue ? "$false" : "$true", idx, idx));
    }

    private static void appendTimePicker(StringBuilder ps, int idx, int tabIdx, int y, String defaultVal) {
        ps.append(String.format(
                "$dtp%d = New-Object System.Windows.Forms.DateTimePicker; " +
                "$dtp%d.Location = New-Object System.Drawing.Point(210,%d); " +
                "$dtp%d.Size = New-Object System.Drawing.Size(100,20); " +
                "$dtp%d.Format = [System.Windows.Forms.DateTimePickerFormat]::Custom; " +
                "$dtp%d.CustomFormat = 'HH:mm'; " +
                "$dtp%d.ShowUpDown = $true; " +
                "$dtp%d.Value = [DateTime]::Today.Add([TimeSpan]::Parse('%s')); " +
                "$tab%d.Controls.Add($dtp%d)\n",
                idx, idx, y, idx, idx, idx, idx, idx, defaultVal, tabIdx, idx));
    }

    private static void appendMultilineTextBox(StringBuilder ps, int idx, int tabIdx, int y, String defaultVal) {
        ps.append(String.format(
                "$txt%d = New-Object System.Windows.Forms.TextBox; " +
                "$txt%d.Location = New-Object System.Drawing.Point(210,%d); " +
                "$txt%d.Size = New-Object System.Drawing.Size(300,60); " +
                "$txt%d.Multiline = $true; " +
                "$txt%d.ScrollBars = 'Vertical'; " +
                "$txt%d.AcceptsReturn = $true; " +
                "$txt%d.Text = \"%s\"; " +
                "$tab%d.Controls.Add($txt%d)\n",
                idx, idx, y, idx, idx, idx, idx, idx, defaultVal, tabIdx, idx));
    }

    private static void appendTextBox(StringBuilder ps, int idx, int tabIdx, int y,
                                      String defaultVal, boolean hasBrowse, boolean isPassword) {
        int width = hasBrowse ? 230 : 300;
        ps.append(String.format(
                "$txt%d = New-Object System.Windows.Forms.TextBox; " +
                "$txt%d.Location = New-Object System.Drawing.Point(210,%d); " +
                "$txt%d.Size = New-Object System.Drawing.Size(%d,20); " +
                "$txt%d.Text = '%s'",
                idx, idx, y, idx, width, idx, defaultVal));
        if (isPassword) {
            ps.append(String.format("; $txt%d.UseSystemPasswordChar = $true", idx));
        }
        ps.append(String.format("; $tab%d.Controls.Add($txt%d)\n", tabIdx, idx));
    }

    private static void appendBrowseButton(StringBuilder ps, int idx, int tabIdx, int y,
                                           String label, boolean isFolder) {
        ps.append(String.format(
                "$btn%d = New-Object System.Windows.Forms.Button; " +
                "$btn%d.Location = New-Object System.Drawing.Point(445,%d); " +
                "$btn%d.Size = New-Object System.Drawing.Size(65,22); " +
                "$btn%d.Text = 'Browse'; " +
                "$tab%d.Controls.Add($btn%d)\n",
                idx, idx, y, idx, idx, tabIdx, idx));

        if (isFolder) {
            ps.append(String.format(
                    "$btn%d.Add_Click({ " +
                    "$dlg = New-Object System.Windows.Forms.FolderBrowserDialog; " +
                    "$dlg.Description = '%s'; " +
                    "$dlg.ShowNewFolderButton = $true; " +
                    "if ($dlg.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { " +
                    "$txt%d.Text = $dlg.SelectedPath } })\n",
                    idx, label, idx));
        } else {
            ps.append(String.format(
                    "$btn%d.Add_Click({ " +
                    "$dlg = New-Object System.Windows.Forms.OpenFileDialog; " +
                    "$dlg.Title = '%s'; " +
                    "$dlg.Filter = 'All Files (*.*)|*.*'; " +
                    "if ($dlg.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { " +
                    "$txt%d.Text = $dlg.FileName } })\n",
                    idx, label, idx));
        }
    }

    // --- SMS hint panel ---

    private static int appendSmsHintPanel(StringBuilder ps, int tabIdx, int y) {
        int s = tabIdx;
        ps.append(String.format(
                "$hintToggle%d = New-Object System.Windows.Forms.LinkLabel; " +
                "$hintToggle%d.Location = New-Object System.Drawing.Point(10,%d); " +
                "$hintToggle%d.Size = New-Object System.Drawing.Size(490,16); " +
                "$hintToggle%d.Text = '+ SMS carrier gateway reference'; " +
                "$tab%d.Controls.Add($hintToggle%d)\n",
                s, s, y, s, s, s, s));
        y += 20;

        ps.append(String.format(
                "$hintPanel%d = New-Object System.Windows.Forms.Panel; " +
                "$hintPanel%d.Location = New-Object System.Drawing.Point(10,%d); " +
                "$hintPanel%d.Size = New-Object System.Drawing.Size(495,230); " +
                "$hintPanel%d.BorderStyle = [System.Windows.Forms.BorderStyle]::FixedSingle; " +
                "$hintPanel%d.BackColor = [System.Drawing.Color]::FromArgb(245,245,250); " +
                "$hintPanel%d.Visible = $false; " +
                "$tab%d.Controls.Add($hintPanel%d)\n",
                s, s, y, s, s, s, s, s, s));

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

        for (String[] row : SMS_CARRIERS) {
            ps.append(String.format("$hintGrid%d.Rows.Add('%s', '%s') | Out-Null\n", s, row[0], row[1]));
        }
        ps.append(String.format("$hintGrid%d.ClearSelection()\n", s));

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

        return y + 5;
    }

    // --- Result collection ---

    private static void appendResultCollection(StringBuilder ps, String resultPath,
                                               List<String> keys, List<Boolean> isBooleanField,
                                               List<Boolean> isTimeField) {
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
            } else if (isTimeField.get(i)) {
                ps.append(String.format(
                        "$out += '%s=' + $dtp%d.Value.ToString('HH:mm')\n",
                        keys.get(i), i));
            } else if (MULTILINE_FIELDS.contains(keys.get(i))) {
                ps.append(String.format(
                        "$out += '%s=' + (($txt%d.Text -split \"`r?`n\" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }) -join ',')\n",
                        keys.get(i), i));
            } else {
                ps.append(String.format("$out += '%s=' + $txt%d.Text\n", keys.get(i), i));
            }
        }
        ps.append(String.format("Set-Content -Path '%s' -Value ($out -join \"`n\")\n", resultPath));
    }

    // --- Value escaping ---

    private static String escapeForMultiline(String value) {
        if (value.contains(",")) {
            value = value.replace(",", "`r`n");
            value = value.replace("$", "`$");
            value = value.replace("\"", "`\"");
        } else {
            value = PowerShellRunner.escapeSingleQuote(value);
        }
        return value;
    }

}
