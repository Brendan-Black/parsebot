# ParseBot

**ParseBot** is a Windows background service that automatically reads purchase-order PDFs from an email inbox, uses Claude AI to extract and match order data against customer and product databases, and uploads structured JSON results to a remote SFTP server.

## What It Does

ParseBot runs as a polling daemon. On a configurable interval (default: 60 seconds), it:

1. Connects to an IMAP mailbox and fetches new messages.
2. Extracts PDF attachments from each email.
3. Looks up per-sender overrides for parsing rules and product lists (see **Per-sender Overrides** below).
4. Sends each PDF to the Anthropic Claude API along with customer and product reference CSVs.
5. Claude parses the purchase order, matches the buyer to a known customer, and maps line items to known products. It returns structured JSON via a tool-call response, or rejects the document if it cannot achieve high-confidence matches.
6. An optional price matrix transforms the unit prices on parsed line items before output.
7. The resulting JSON is uploaded to a configured SFTP server.
8. The original email is moved to a "Processed" or "Failed" IMAP folder depending on outcome.
9. Pipeline outcomes are published on an internal event bus that drives failure alerts and daily report cards.

## Architecture

ParseBot is written in Java 25 and organized as a pipeline with clearly separated stages:

| Stage | Responsibility |
|-------|----------------|
| **Read** | Poll a data source (email, filesystem, or SFTP) and return raw documents |
| **Parse** | Extract PDFs, call the Claude Messages API with reference CSVs, and capture the structured response |
| **Write** | Deliver JSON results to their destination and route source documents to success/failure folders |
| **Orchestrate** | Wire the stages together, handle per-item errors, and log outcomes |

The service runs on a scheduled interval with graceful shutdown handling. An `EventBus` publishes structured events (currently consecutive-failure alerts and daily report cards) to a `NotificationDispatcher` that routes them to configured channels by severity.

## Per-sender Overrides

Two directories let an operator tweak parsing behavior for specific senders without redeploying:

- `custom_rules/` — text files that append extra instructions to the Claude prompt.
- `custom_productlists/` — CSV files used in place of the global product list.

Files are keyed by sender email. Lookup order is exact match (`person@place.com`) then domain wildcard (`*@place.com`); if neither exists the global defaults are used.

## Price Matrix

An optional CSV (`csv.pricematrix`) transforms unit prices on parsed line items. Columns: `customer_number,product_id,type,value`. Supported rule types are `multiplier`, `fixed`, `percentage_discount`, and `percentage_markup`. `*` acts as a wildcard for customer or product, and the most specific matching rule wins.

## Events and Notifications

The service emits events for two conditions:

- **Consecutive failures** — N back-to-back processing failures (threshold configurable) fire a `CONSECUTIVE_FAILURES` critical event.
- **Daily report card** — a scheduled `REPORT_CARD` info event fires once per day at a configured time summarizing successes and failures.

Events are dispatched to notification channels by severity:

- **Critical** events go to a Microsoft Teams webhook.
- **Info** events go to an SMTP recipient list (the SMTP channel reuses the IMAP host and credentials by default).

Both channels are independently enabled/disabled and tolerate failure: a broken channel is logged but does not crash the pipeline.

## Configuration

Key configuration groups:

- **Scheduling** -- poll interval.
- **Mail** -- IMAP host, port, credentials, protocol, source folder, and success/failure folder names. SMTP notifications reuse these settings unless explicitly overridden.
- **Claude** -- Anthropic API key. The service calls `claude-sonnet-4-20250514` via the Messages API with PDF document and CSV tool context.
- **Reference data** -- paths to customer CSV, product CSV, optional price matrix CSV, and directories for per-sender custom rules and product lists.
- **SFTP output** -- host, port, credentials (password or SSH private key), and remote upload directory.
- **Events** -- consecutive-failure threshold and daily report-card time.
- **Notifications** -- SMTP enable flag, recipient lists (regular + urgent), STARTTLS toggle; Teams webhook enable flag and URL.

## Packaging and Deployment

ParseBot is a multi-module Gradle project (Java 25 toolchain) with three deliverables:

### Service

The core daemon. A PowerShell packaging script (`package.ps1`) uses `jpackage` to produce a self-contained app-image distribution under `dist/service/` that includes the application JARs, dependencies, and a bundled JVM runtime. No external Java installation is required on the target machine.

### Installer

A companion tool packaged the same way. Double-clicking the installer opens a GUI that walks the user through setup:

1. A launcher dialog asks whether to **Install** or **Uninstall**.
2. On install, a configuration dialog collects all required settings (mail server, API key, SFTP credentials, reference CSVs, event thresholds, notification targets, etc.).
3. The service is registered, configured, and started automatically.

Internally the installer is split into focused components: `InstallerConfig` defines the configuration field set, `PowerShellGui` drives the Swing dialogs, and `ServiceController` wraps the `sc.exe` calls used to register, start, and stop the Windows service.

### Check-Events

A standalone GUI log viewer, also packaged via `jpackage`. It scans the service's log directory for `EVENT:` markers, parses their JSON payloads, and displays the events (consecutive-failure alerts, daily report cards) in a Swing window. Useful for operators who want to review recent pipeline health without tailing raw log files.

## Dependencies

| Library | Purpose |
|---------|---------|
| Jakarta Mail + Angus Mail | IMAP/IMAPS email access and SMTP notifications |
| JSch | SFTP file transfer |
| Gson | JSON parsing for the price matrix and event log viewer |
| SLF4J + Logback | Logging (console + daily-rotating file, 30-day retention) |

The Claude API integration and the Teams webhook notifier both use the JDK's built-in `java.net.http.HttpClient` with no additional HTTP library.

## Logging

All activity is logged to both the console and a rolling file at `logs/parsebot.log`. Log files rotate daily and are retained for 30 days.
