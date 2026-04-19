# ParseBot

**ParseBot** is a Windows background service that automatically reads purchase-order PDFs from an email inbox, uses Claude AI to extract and match order data against customer and product databases, and uploads structured JSON results to a remote SFTP server. It ships with a web-based admin console for setup, monitoring, interactive testing, and in-place updates.

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
9. A per-sender processing-history record is persisted to disk so repeated deliveries can be tracked across restarts.
10. Pipeline outcomes are published on an internal event bus that drives audit logging, failure alerts, and daily report cards.

## Architecture

ParseBot is written in Java 25 and organized as a pipeline with clearly separated stages:

| Stage | Responsibility |
|-------|----------------|
| **Read** | Poll a data source (IMAP mailbox) and return raw documents |
| **Parse** | Extract PDFs, call the Claude Messages API with reference CSVs, and capture the structured response |
| **Write** | Deliver JSON results to SFTP and route source documents to success/failure IMAP folders |
| **Orchestrate** | Wire the stages together, handle per-item errors, apply per-sender overrides and the price matrix, and log outcomes |

The service runs on a scheduled interval with graceful shutdown handling. An `EventBus` publishes structured events to a `NotificationDispatcher` that routes them to configured channels by severity. Events are also written to the log file with a machine-readable `EVENT:` marker so the admin console can replay them.

## Modules

ParseBot is a multi-module Gradle project with three modules:

- **`shared`** — core abstractions reused by both executables: `ClaudeClient`, `PriceMatrix`, the event system (`Event`, `EventType`, `EventPublisher`), the persistence layer (`ProcessingHistoryRepository` + JSON-file implementation), and `ConfigKey` constants.
- **`service`** — the headless daemon. Loads `AppConfig`, builds notification channels, wires the pipeline (`ParseBotService`, `MailboxReader`, `MailboxProcessor`, `SftpWriter`), and schedules both the main poll loop and the daily report-card checker.
- **`admin`** — a local web application that configures, installs, monitors, and updates the service (see **Admin Console** below). Includes an embedded HTTP server and a Preact + Vite + TypeScript frontend that is built during the Gradle build and bundled as a JAR resource.

## Per-sender Overrides

Two directories let an operator tweak parsing behavior for specific senders without redeploying:

- `custom_rules/` — text files that append extra instructions to the Claude prompt.
- `custom_productlists/` — CSV files used in place of the global product list.

Files are keyed by sender email. Lookup order is exact match (`person@place.com`) then domain wildcard (`*@place.com`); if neither exists the global defaults are used. Both directory paths are configurable.

## Price Matrix

An optional CSV (`csv.pricematrix`) transforms unit prices on parsed line items. Columns: `customer_number,product_id,type,value`. Supported rule types are `multiplier`, `fixed`, `percentage_discount`, and `percentage_markup`. `*` acts as a wildcard for customer or product, and the most specific matching rule wins.

## Persistence

A `ProcessingHistoryRepository` records, per sender email, the outcomes of recent deliveries. The default implementation (`JsonFileProcessingHistoryRepository`) writes to `state/processing-history.json` and caps the history at a configurable number of records per email address (default 10) to bound file size. The repository is an interface so a database-backed implementation can be swapped in later without changing callers.

## Events and Notifications

The service emits a rich set of structured events:

- **Critical** — `CONSECUTIVE_FAILURES` (N back-to-back processing failures; threshold configurable).
- **Info** — `REPORT_CARD` (scheduled daily summary of successes and failures).
- **Audit** — `MESSAGE_PROCESSING_FAILED`, `PIPELINE_RUN_FAILED`, `CLAUDE_API_FAILED`, `PDF_VALIDATION_FAILED`, `SFTP_WRITE_FAILED`, `MAILBOX_READ_FAILED`, `MAILBOX_WRITE_FAILED`, `NOTIFICATION_FAILED`.

Events are dispatched to notification channels by severity:

- **Critical** events go to a Microsoft Teams webhook and to an "urgent" SMTP recipient list.
- **Info** events go to the regular SMTP recipient list (the SMTP channel reuses the IMAP host and credentials by default).
- **Audit** events are logged but not sent to channels; they are available for review in the admin console.

Both channels are independently enabled/disabled and tolerate failure: a broken channel is logged but does not crash the pipeline.

## Admin Console

The `admin` executable is a local web application that replaces the earlier Swing installer and log-viewer tools. Launching it starts an embedded `HttpServer` bound to 127.0.0.1 on an ephemeral port and opens the default browser to the Preact frontend. The console provides:

- **Launcher** — navigation hub for the other pages.
- **Install** — a multi-section configuration form (mail, Claude, SFTP, reference data, events, notifications) that validates input, writes `parsebot.properties`, and registers + starts the Windows service.
- **Uninstall** — stop and unregister the service.
- **Status** — whether the service is installed and running.
- **Events** — a searchable log viewer that parses the `EVENT:` markers from `logs/parsebot*.log` and filters by type and date range.
- **Sandbox** — interactive PDF-parsing playground. Load a PDF from disk or from the mailbox, supply customer/product CSVs and custom rules, and see Claude's raw response without writing anything to SFTP.
- **Updates** — check GitHub for newer releases and apply them in place (see **Updates** below).

The admin binary also accepts CLI subcommands (`ui`, `install`, `uninstall`, `status`, `events`) so scripted workflows skip the browser. The Windows-service lifecycle is handled via `sc.exe` wrappers.

All UI ↔ backend traffic goes through a small REST API (`/api/config`, `/api/service/...`, `/api/events`, `/api/sandbox`, `/api/reference-data`, `/api/email-pdfs`, `/api/update/...`, `/api/file-chooser`, etc.) served by `AdminServer`. A `--demo` flag runs the admin against in-memory fixtures for demos and development.

## Updates

ParseBot ships with a GitHub-backed auto-updater. The admin console's Updates page queries the GitHub Releases API (authenticated with a configured PAT) for newer versions of the distribution, downloads the release zip, and applies it via a bundled PowerShell script that stops the service, replaces files, and restarts the service. Version information is baked into the service JAR at build time (`version.properties`).

## Configuration

Key configuration groups (all backed by `parsebot.properties` in the working directory, with a classpath fallback and optional `-Dparsebot.<key>` system-property overrides):

- **Scheduling** — poll interval.
- **Mail** — IMAP host, port, credentials, protocol, source folder, and success/failure folder names. SMTP notifications reuse these settings unless explicitly overridden.
- **Claude** — Anthropic API key and PDF size/page limits. The service calls `claude-sonnet-4-20250514` via the Messages API with PDF document and CSV tool context.
- **Reference data** — paths to customer CSV, product CSV, optional price matrix CSV, and directories for per-sender custom rules and product lists.
- **SFTP output** — host, port, credentials (password or SSH private key), and remote upload directory.
- **Persistence** — state directory (default `state/`) and per-email history cap.
- **Events** — consecutive-failure threshold and daily report-card time.
- **Notifications** — SMTP enable flag, recipient lists (regular + urgent), STARTTLS toggle; Teams webhook enable flag and URL.
- **Updates** — GitHub repository owner/name and PAT.

## Packaging and Deployment

ParseBot produces two `jpackage` app-image deliverables, both with a bundled JVM runtime (no external Java installation required on the target machine):

- **`service`** — the headless daemon registered as a Windows service.
- **`admin`** — the configuration and monitoring console.

A PowerShell packaging script (`package.ps1`) builds both, merges them into a single `dist/ParseBot/` folder, and zips it as `dist/ParseBot.zip`. A companion `release.ps1` verifies the declared version, runs `package.ps1`, and publishes the zip to GitHub Releases via `gh`.

## Dependencies

| Library | Purpose |
|---------|---------|
| Jakarta Mail + Angus Mail | IMAP/IMAPS email access and SMTP notifications |
| JSch | SFTP file transfer |
| Gson | JSON parsing (price matrix, event log viewer, admin REST API, processing history) |
| SLF4J + Logback | Logging (console + daily-rotating file, 30-day retention) |

The Claude API integration, the Teams webhook notifier, the GitHub update checker, and the admin console's embedded web server all use JDK built-ins (`java.net.http.HttpClient`, `com.sun.net.httpserver.HttpServer`) with no additional HTTP library.

The admin frontend is built with Preact, Vite, and TypeScript; the compiled assets are embedded as JAR resources and served from the admin binary — no external web server or Node runtime is needed at runtime.

## Logging

All activity is logged to both the console and a rolling file at `logs/parsebot.log`. Log files rotate daily and are retained for 30 days. Structured events are emitted with a leading `EVENT:` marker so the admin console's Events page can reconstruct them from the log files.
