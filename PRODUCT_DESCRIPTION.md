# ParseBot

**ParseBot** is a Windows background service that automatically reads purchase-order PDFs from an email inbox, uses Claude AI to extract and match order data against customer and product databases, and uploads structured JSON results to a remote SFTP server.

## What It Does

ParseBot runs as a polling daemon. On a configurable interval (default: 60 seconds), it:

1. Connects to an IMAP mailbox and fetches new messages.
2. Extracts PDF attachments from each email.
3. Sends each PDF to the Anthropic Claude API along with customer and product reference CSVs.
4. Claude parses the purchase order, matches the buyer to a known customer, and maps line items to known products. It returns structured JSON via a tool-call response, or rejects the document if it cannot achieve high-confidence matches.
5. The resulting JSON is uploaded to a configured SFTP server.
6. The original email is moved to a "Processed" or "Failed" IMAP folder depending on outcome.

Filesystem and SFTP input sources are also supported as alternatives to email.

## Architecture

The service is organized as a pipeline with clearly separated stages:

| Stage | Component | Responsibility |
|-------|-----------|----------------|
| **Read** | `MailboxReader`, `FileSystemReader`, `SftpReader` | Poll a data source and return raw documents |
| **Model** | `RawData` (sealed hierarchy) | Carry document bytes and source metadata through the pipeline |
| **Parse** | `DataParser`, `ClaudeClient` | Extract PDFs, call the Claude Messages API with reference CSVs, capture the structured response |
| **Transform** | `TransformedData` | Pair a sanitized filename with the JSON byte payload |
| **Write** | `SftpWriter`, `MailboxWriter`, `FileSystemWriter` | Deliver results to their destination and route source documents to success/failure folders |
| **Orchestrate** | `ParseBotService` | Wire the stages together, handle per-item errors, and log outcomes |

`Main` starts a `ScheduledExecutorService` that invokes the service on a fixed interval and registers a shutdown hook for graceful teardown.

## Configuration

All settings live in a Java properties file (`parsebot.properties`). Values can be supplied from three sources, in priority order:

1. An external file in the working directory.
2. The classpath-bundled default file.
3. JVM system properties (`-Dparsebot.<key>=<value>`).

Key configuration groups:

- **Scheduling** -- poll interval.
- **Mail** -- IMAP host, port, credentials, protocol, source folder, and success/failure folder names.
- **Claude** -- Anthropic API key. The service calls `claude-sonnet-4-20250514` via the Messages API with PDF document and CSV tool context.
- **Reference data** -- paths to customer and product CSV files.
- **SFTP output** -- host, port, credentials (password or SSH private key), and remote upload directory.
- **Filesystem** -- local input directory and glob pattern (alternative to email input).

## Packaging and Deployment

ParseBot is a multi-module Gradle project (Java 25 toolchain) with two deliverables:

### Service

The core daemon. A PowerShell packaging script (`package.ps1`) uses `jpackage` to produce a self-contained app-image distribution under `dist/service/` that includes the application JARs, dependencies, and a bundled JVM runtime. No external Java installation is required on the target machine.

### Installer

A companion CLI tool packaged the same way under `dist/installer/`. It provides three commands:

| Command | Effect |
|---------|--------|
| `install` | Launches a PowerShell WinForms dialog collecting all 20 configuration fields, then registers ParseBot as an auto-start Windows service via `sc.exe`, injecting settings as system properties on the service binary path. |
| `uninstall` | Stops and removes the Windows service. |
| `status` | Queries and prints the current service state. |

## Dependencies

| Library | Purpose |
|---------|---------|
| Jakarta Mail + Angus Mail | IMAP/IMAPS email access |
| JSch | SFTP file transfer |
| SLF4J + Logback | Logging (console + daily-rotating file, 30-day retention) |

The Claude API integration uses the JDK's built-in `java.net.http.HttpClient` with no additional HTTP library.

## Logging

All activity is logged to both the console and a rolling file at `logs/parsebot.log`. Log files rotate daily and are retained for 30 days.
