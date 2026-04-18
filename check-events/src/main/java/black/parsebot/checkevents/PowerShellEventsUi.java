package black.parsebot.checkevents;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import black.parsebot.event.Event;
import black.parsebot.event.EventType;
import black.parsebot.ps.PowerShellRunner;
import black.parsebot.ps.WinFormsScript;
import black.parsebot.storage.Storage;

final class PowerShellEventsUi implements EventsUi {

  private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneId.systemDefault());
  private static final long NET_EPOCH_TICKS = 621355968000000000L;

  private final Storage<Event> storage;
  private final Path logDir;

  PowerShellEventsUi(Storage<Event> storage, Path logDir) {
    this.storage = storage;
    this.logDir = logDir;
  }

  @Override
  public void showEmpty(Path logDir) {
    renderEmpty("No events found in: " + logDir);
  }

  @Override
  public void showEvents(List<Event> events, Path logDir) {
    renderViewer(events, null, null, null);
  }

  @Override
  public void searchEvents(EventType type, ZonedDateTime start, ZonedDateTime end) {
    List<Event> all;
    try {
      all = storage.readAll();
    } catch (IOException e) {
      System.err.println("Failed to read events for search: " + e.getMessage());
      return;
    }
    if (all.isEmpty()) {
      renderEmpty("No events found in: " + logDir);
      return;
    }
    renderViewer(all, type, start, end);
  }

  private void renderViewer(List<Event> events,
      EventType initialType,
      ZonedDateTime initialStart,
      ZonedDateTime initialEnd) {
    ZonedDateTime earliest = eventBoundary(events, true);
    ZonedDateTime latest = eventBoundary(events, false);
    ZonedDateTime fromDefault = initialStart != null ? initialStart : earliest;
    ZonedDateTime toDefault = initialEnd != null ? initialEnd : latest;
    boolean hasInitialFilter = initialType != null || initialStart != null || initialEnd != null;

    StringBuilder ps = new StringBuilder();
    ps.append(PowerShellRunner.WINFORMS_PREAMBLE).append('\n');

    WinFormsScript.form(ps, "form",
        "ParseBot Events (" + events.size() + " entries)",
        900, 560, false, true);
    ps.append("$form.MinimumSize = New-Object System.Drawing.Size(700,360)\n\n");

    ps.append("$logDir = '").append(PowerShellRunner.escapeSingleQuote(logDir.toString())).append("'\n\n");

    // Search panel
    ps.append("$panel = New-Object System.Windows.Forms.Panel\n");
    ps.append("$panel.Location = New-Object System.Drawing.Point(10,10)\n");
    ps.append("$panel.Size = New-Object System.Drawing.Size(875,60)\n");
    ps.append("$panel.Anchor = 'Top,Left,Right'\n");
    ps.append("$form.Controls.Add($panel)\n\n");

    appendLabel(ps, "lblType", "Type:", 0, 10, 40, 20);
    appendComboBox(ps, "cboType", 45, 8, 200, 25);
    ps.append("[void]$cboType.Items.Add('All')\n");
    for (EventType t : EventType.values()) {
      ps.append("[void]$cboType.Items.Add('").append(t.name()).append("')\n");
    }
    String initialTypeName = initialType != null ? initialType.name() : "All";
    ps.append("$cboType.SelectedItem = '").append(initialTypeName).append("'\n\n");

    appendLabel(ps, "lblFrom", "From:", 260, 10, 40, 20);
    appendDateTimePicker(ps, "dtpFrom", 305, 8, 160, 25, fromDefault);

    appendLabel(ps, "lblTo", "To:", 475, 10, 25, 20);
    appendDateTimePicker(ps, "dtpTo", 505, 8, 160, 25, toDefault);

    appendButton(ps, "btnSearch", "Search", 680, 7, 80, 27);
    appendButton(ps, "btnReset", "Reset", 770, 7, 80, 27);
    ps.append('\n');

    // Reset anchors — full unfiltered bounds
    ps.append("$resetFrom = ").append(dateTimeLiteral(earliest)).append('\n');
    ps.append("$resetTo = ").append(dateTimeLiteral(latest)).append("\n\n");

    // Data grid
    WinFormsScript.dataGridView(ps, "dgv", "form", 10, 80, 875, 410,
        List.of("Timestamp", "Type", "Severity", "Message", "Details"),
        List.of(15, 15, 10, 30, 30));
    ps.append("$dgv.Anchor = 'Top,Bottom,Left,Right'\n\n");

    // Status bar
    ps.append("$status = New-Object System.Windows.Forms.StatusBar\n");
    ps.append("$status.Text = \"Logs: $logDir\"\n");
    ps.append("$form.Controls.Add($status)\n\n");

    // Events data (newest first)
    ps.append("$allEvents = New-Object System.Collections.ArrayList\n");
    for (int i = events.size() - 1; i >= 0; i--) {
      appendEventRecord(ps, events.get(i));
    }
    ps.append('\n');

    ps.append("""
        function Show-Events($rows) {
          $dgv.Rows.Clear()
          foreach ($e in $rows) {
            $idx = $dgv.Rows.Add($e.Timestamp, $e.Type, $e.Severity, $e.Message, $e.Details)
            if ($e.Severity -eq 'CRITICAL') {
              $dgv.Rows[$idx].DefaultCellStyle.BackColor = [System.Drawing.Color]::MistyRose
              $dgv.Rows[$idx].DefaultCellStyle.ForeColor = [System.Drawing.Color]::DarkRed
            }
          }
          $status.Text = "Logs: $logDir  |  Showing $($rows.Count) of $($allEvents.Count) events"
        }

        function Apply-Filter {
          $selType = $cboType.SelectedItem
          $fromTicks = $dtpFrom.Value.Ticks
          $toTicks = $dtpTo.Value.Ticks
          $filtered = New-Object System.Collections.ArrayList
          foreach ($e in $allEvents) {
            if ($selType -ne 'All' -and $e.Type -ne $selType) { continue }
            if ($e.TsTicks -lt $fromTicks -or $e.TsTicks -gt $toTicks) { continue }
            [void]$filtered.Add($e)
          }
          Show-Events $filtered
        }

        $btnSearch.Add_Click({ Apply-Filter })
        $btnReset.Add_Click({
          $cboType.SelectedItem = 'All'
          $dtpFrom.Value = $resetFrom
          $dtpTo.Value = $resetTo
          Apply-Filter
        })

        """);

    ps.append(hasInitialFilter ? "Apply-Filter\n" : "Show-Events $allEvents\n");
    ps.append("\n[void]$form.ShowDialog()\n");

    runPowerShell(ps.toString());
  }

  private static void appendLabel(StringBuilder ps, String var, String text, int x, int y, int w, int h) {
    String v = "$" + var;
    ps.append(v).append(" = New-Object System.Windows.Forms.Label\n");
    ps.append(v).append(".Text = '").append(PowerShellRunner.escapeSingleQuote(text)).append("'\n");
    ps.append(v).append(".Location = New-Object System.Drawing.Point(").append(x).append(",").append(y).append(")\n");
    ps.append(v).append(".Size = New-Object System.Drawing.Size(").append(w).append(",").append(h).append(")\n");
    ps.append("$panel.Controls.Add(").append(v).append(")\n");
  }

  private static void appendComboBox(StringBuilder ps, String var, int x, int y, int w, int h) {
    String v = "$" + var;
    ps.append(v).append(" = New-Object System.Windows.Forms.ComboBox\n");
    ps.append(v).append(".DropDownStyle = 'DropDownList'\n");
    ps.append(v).append(".Location = New-Object System.Drawing.Point(").append(x).append(",").append(y).append(")\n");
    ps.append(v).append(".Size = New-Object System.Drawing.Size(").append(w).append(",").append(h).append(")\n");
    ps.append("$panel.Controls.Add(").append(v).append(")\n");
  }

  private static void appendDateTimePicker(StringBuilder ps, String var, int x, int y, int w, int h,
      ZonedDateTime initial) {
    String v = "$" + var;
    ps.append(v).append(" = New-Object System.Windows.Forms.DateTimePicker\n");
    ps.append(v).append(".Format = 'Custom'\n");
    ps.append(v).append(".CustomFormat = 'yyyy-MM-dd HH:mm:ss'\n");
    ps.append(v).append(".Location = New-Object System.Drawing.Point(").append(x).append(",").append(y).append(")\n");
    ps.append(v).append(".Size = New-Object System.Drawing.Size(").append(w).append(",").append(h).append(")\n");
    ps.append(v).append(".Value = ").append(dateTimeLiteral(initial)).append('\n');
    ps.append("$panel.Controls.Add(").append(v).append(")\n");
  }

  private static void appendButton(StringBuilder ps, String var, String text, int x, int y, int w, int h) {
    String v = "$" + var;
    ps.append(v).append(" = New-Object System.Windows.Forms.Button\n");
    ps.append(v).append(".Text = '").append(PowerShellRunner.escapeSingleQuote(text)).append("'\n");
    ps.append(v).append(".Location = New-Object System.Drawing.Point(").append(x).append(",").append(y).append(")\n");
    ps.append(v).append(".Size = New-Object System.Drawing.Size(").append(w).append(",").append(h).append(")\n");
    ps.append("$panel.Controls.Add(").append(v).append(")\n");
  }

  private static void appendEventRecord(StringBuilder ps, Event event) {
    String timestamp = event.timestamp() != null ? DISPLAY_FORMAT.format(event.timestamp()) : "N/A";
    String type = event.type() != null ? event.type().name() : "N/A";
    String severity = event.severity() != null ? event.severity().name() : "N/A";
    String message = event.message() != null ? event.message() : "";
    String details = formatDetails(event.details());
    long ticks = toNetTicks(event.timestamp());

    ps.append("[void]$allEvents.Add([PSCustomObject]@{ ");
    ps.append("Timestamp='").append(PowerShellRunner.escapeSingleLine(timestamp)).append("'; ");
    ps.append("Type='").append(PowerShellRunner.escapeSingleLine(type)).append("'; ");
    ps.append("Severity='").append(PowerShellRunner.escapeSingleLine(severity)).append("'; ");
    ps.append("Message='").append(PowerShellRunner.escapeSingleLine(message)).append("'; ");
    ps.append("Details='").append(PowerShellRunner.escapeSingleLine(details)).append("'; ");
    ps.append("TsTicks=[long]").append(ticks);
    ps.append(" })\n");
  }

  private static String dateTimeLiteral(ZonedDateTime zdt) {
    LocalDateTime ldt = zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    return String.format("[DateTime]::new(%d,%d,%d,%d,%d,%d)",
        ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
        ldt.getHour(), ldt.getMinute(), ldt.getSecond());
  }

  private static long toNetTicks(Instant instant) {
    if (instant == null) return 0L;
    return instant.getEpochSecond() * 10_000_000L + instant.getNano() / 100 + NET_EPOCH_TICKS;
  }

  private static ZonedDateTime eventBoundary(List<Event> events, boolean earliest) {
    Instant boundary = events.stream()
        .map(Event::timestamp)
        .filter(Objects::nonNull)
        .reduce((a, b) -> earliest
            ? (a.isBefore(b) ? a : b)
            : (a.isAfter(b) ? a : b))
        .orElse(earliest ? Instant.EPOCH : Instant.now());
    return boundary.atZone(ZoneId.systemDefault());
  }

  private static void renderEmpty(String message) {
    StringBuilder ps = new StringBuilder();
    ps.append(PowerShellRunner.WINFORMS_PREAMBLE).append('\n');
    WinFormsScript.form(ps, "form", "ParseBot Events", 400, 120, true, true);
    WinFormsScript.label(ps, "lbl", "form", 10, 20, 380, 40, message);
    WinFormsScript.button(ps, "btnOk", "form", 160, 70, 80, 30, "OK", "OK");
    ps.append("$form.AcceptButton = $btnOk\n");
    ps.append("[void]$form.ShowDialog()\n");

    runPowerShell(ps.toString());
  }

  private static String formatDetails(Map<String, String> details) {
    if (details == null || details.isEmpty())
      return "";
    StringBuilder sb = new StringBuilder();
    details.forEach((k, v) -> {
      if (!sb.isEmpty())
        sb.append("; ");
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
