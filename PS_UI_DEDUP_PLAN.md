# PowerShell WinForms UI Deduplication — Execution Plan

Self-contained plan for a fresh Claude Code session. Start from a clean working tree at commit `fffde67` ("updated config partitioning"). No prior context required.

## Current state

Two files build Windows Forms UI by concatenating PowerShell script fragments:

- `installer/src/main/java/black/parsebot/installer/PowerShellInstallerUi.java` (~431 lines) — launcher dialog + tabbed config dialog with fields and browse buttons.
- `check-events/src/main/java/black/parsebot/checkevents/PowerShellEventsUi.java` (~140 lines) — empty-state dialog + event log viewer with DataGridView.

Already shared in `shared/src/main/java/black/parsebot/ps/PowerShellRunner.java`:
- `WINFORMS_PREAMBLE` constant
- `escapeSingleQuote(String)`
- `escapeSingleLine(String)`
- `run(...)` / `runWithResult(...)`

Both UI files duplicate the PS construction patterns for: form header, button, label, DataGridView, status bar.

## Goal

Extract the genuinely-duplicated WinForms construction snippets into a shared helper. **Do not** build a DSL or rewrite installer-specific field types.

Target: eliminate ~50–80 lines of duplicated string building between the two files without adding a new abstraction layer the project doesn't need.

## Design

Create `shared/src/main/java/black/parsebot/ps/WinFormsScript.java`:

- `public final class WinFormsScript` with a private constructor (utility class).
- Static methods that append PS snippets to a caller-supplied `StringBuilder`.
- No new dependencies.

### Method signatures

```java
public static void form(StringBuilder ps, String varName, String title,
                        int width, int height,
                        boolean fixedDialog, boolean topMost);

public static void label(StringBuilder ps, String varName, String parentVar,
                         int x, int y, int w, int h, String textLiteral);

public static void button(StringBuilder ps, String varName, String parentVar,
                          int x, int y, int w, int h, String textLiteral,
                          String dialogResult);  // null = no DialogResult

public static void dataGridView(StringBuilder ps, String varName, String parentVar,
                                int x, int y, int w, int h,
                                List<String> columnNames, List<Integer> fillWeights);

public static void statusBar(StringBuilder ps, String varName, String parentVar,
                             String textLiteral);
```

### Conventions

- `textLiteral` params are raw strings — helpers escape via `PowerShellRunner.escapeSingleQuote(...)` and emit single-quoted PS literals.
- `varName` / `parentVar` are PS variable names **without** the `$` (helper adds it). Example: `form(ps, "form", ...)` emits `$form = New-Object ...`.
- Every helper ends with `$<parentVar>.Controls.Add($<varName>)\n` except `form` (which is the top-level container).
- The `button` helper emits `$form.AcceptButton`/`CancelButton` wiring **only if** `dialogResult` is `"OK"` or `"Cancel"` AND the caller indicates it; to keep scope tight, skip auto-wiring — callers append `$form.AcceptButton = $btn` themselves when needed (matches installer's current behavior).
- The `dataGridView` helper emits the **common** settings: `ReadOnly = $true`, `AllowUserToAddRows = $false`, `AllowUserToDeleteRows = $false`, `AutoSizeColumnsMode = 'Fill'`, `SelectionMode = 'FullRowSelect'`, `RowHeadersVisible = $false`, plus the column list with fill weights. Callers append any additional styling (colors, extra flags) locally.

## Changes by file

### `shared/src/main/java/black/parsebot/ps/WinFormsScript.java` (new)

Implement the five helpers above. Each helper uses `String.format` with the passed-in `varName` / `parentVar` / dims / escaped literal. Keep each helper ~10–20 lines. No tests in this pass.

### `check-events/.../PowerShellEventsUi.java`

Refactor targets (line numbers approximate — re-read file before editing):

- `showEmpty()` body that declares `$form`, `$lbl`, `$btnOk`: replace the three `New-Object` blocks (lines ~22–42 inside the text block) with equivalent calls to `WinFormsScript.form/label/button`. Since `showEmpty` currently uses a `"""..."""` text block, convert it to a `StringBuilder` built with the helpers, matching `showEvents`'s style.
- `showEvents()`:
  - Lines ~55–60: form construction → `WinFormsScript.form(ps, "form", ...)`. Keep the `$form.MinimumSize` line local (uncommon setting).
  - Lines ~63–86: DataGridView construction + columns + fill weights → `WinFormsScript.dataGridView(ps, "dgv", "form", 10, 10, 875, 440, List.of("Timestamp","Type","Severity","Message","Details"), List.of(15,15,10,30,30))`. Keep `$dgv.Anchor = 'Top,Bottom,Left,Right'` local (installer's hint panel doesn't use it).
  - Lines ~114–118: status bar → `WinFormsScript.statusBar(ps, "status", "form", "Logs: " + logDir)`.
- **Leave untouched**: the per-row `.Rows.Add` loop, MistyRose critical-row styling, `formatDetails`, `runPowerShell`.

### `installer/.../PowerShellInstallerUi.java`

- `appendFormSetup` (line ~170): replace the `$form = ...` block with `WinFormsScript.form(ps, "form", "ParseBot Service Configuration", 560, 400, true, false)`. Keep the TabControl (`$tabs = ...`) setup local — tabs are installer-only and not worth a helper.
- `appendFormButtons` (line ~193): replace the two inline button blocks with `WinFormsScript.button(...)` calls for OK and Cancel. Append the `$form.AcceptButton = $btnOk` and `$form.CancelButton = $btnCancel` lines after each call (they remain installer-local).
- `showLauncherDialog()` (line 18): the launcher uses a text block with `.formatted(...)`. Convert to `StringBuilder` + helpers for `form`, `label`, two `button`s. Keep the click-handler blocks (`Add_Click { Set-Content ... }`) and FormClosed handler inline — they're launcher-specific logic.
- `appendSmsHintPanel` (line ~323), DataGridView construction (lines ~345–365): replace with `WinFormsScript.dataGridView(ps, "hintGrid" + s, "hintPanel" + s, 0, 0, 493, 228, List.of("Gateway","Carrier"), List.of(55,45))`. Then append SMS-specific lines locally: `Anchor`, `AllowUserToResizeRows`, `BackgroundColor`, `BorderStyle`, `ColumnHeadersDefaultCellStyle.BackColor`, `EnableHeadersVisualStyles`. The helper's column-name arg takes the *Name*; the installer currently passes a display header as the second arg (`Add('Gateway', 'Gateway Address')`). **Decision**: have the helper accept only column names and emit `Columns.Add(name, name)`. Installer gets slightly different header text ("Gateway"/"Carrier" instead of "Gateway Address"/"Carrier") — if this is unacceptable, extend the helper to take `List<String[]>` of `{name, header}` pairs. Default to the simpler signature unless the header difference is user-visible and matters.
- **Leave untouched**: `appendLabel` (has boolean-trimming logic), `appendBooleanRadios`, `appendTimePicker`, `appendMultilineTextBox`, `appendTextBox`, `appendBrowseButton`, `appendResultCollection`, `escapeForMultiline`, `appendTabPage`, the tab-switching / hint-toggle link-label logic.

## Not in scope

- No fluent builder / DSL.
- No control-object model (Form, Button classes).
- No changes to `InstallerUi` / `EventsUi` interfaces.
- No tests (separate task — the project has zero tests across all modules; a dedicated test-coverage pass is more valuable than sprinkling tests here).
- No changes to `PowerShellRunner`.
- No changes to installer's `escapeForMultiline` (its backtick-escaping logic is genuinely installer-specific and not duplicated).

## Verification

1. `./gradlew compileJava` — must pass.
2. Quick diff-check: for one of the simpler dialogs (e.g. `showEmpty`), print the generated PS script before and after and confirm it's equivalent (extra/missing whitespace is fine; control properties must match).
3. If Windows + PowerShell available, optionally `./gradlew :check-events:run` and confirm the empty-state dialog renders. Same for `./gradlew :installer:run` if straightforward. Skip if blocked — compile success + the diff check are enough.

## Commit

Single commit, message:

```
extract shared WinForms script helpers

Move form/button/label/DataGridView/statusBar construction
into shared black.parsebot.ps.WinFormsScript. Used by both
PowerShellInstallerUi and PowerShellEventsUi.
```

## Judgment calls for the fresh session

- If a helper signature feels cramped, add an overload rather than forcing the caller to hand-emit raw PS.
- If the SMS hint DataGridView needs so much extra installer-specific styling that `WinFormsScript.dataGridView` plus tail-appended lines reads worse than the original inline version, leave it inline and note it in the commit body.
- If `showLauncherDialog`'s text-block-to-StringBuilder conversion becomes messy (click handlers are awkward), leave it as a text block and only swap out the form/label/button sub-sections. Prefer a working mixed style over a forced-uniform ugly rewrite.
