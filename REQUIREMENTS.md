# ParseBot Requirements and Installation Guide

## Prerequisites

Before installing ParseBot, ensure you have the following:

- **Windows 10 or later** -- ParseBot runs as a Windows service.
- **Administrator privileges** -- required to register and manage Windows services.
- **An IMAP-enabled email account** -- the mailbox ParseBot will monitor for incoming purchase orders. You will need the server hostname, port, and login credentials.
- **An Anthropic API key** -- ParseBot uses Claude to parse documents. Obtain a key from [console.anthropic.com](https://console.anthropic.com).
- **Customer and product CSV files** -- reference data that Claude uses to match buyers and line items. These files must be accessible from the machine where ParseBot is installed.
- **An SFTP server** -- the destination where parsed JSON results will be uploaded. You will need the hostname, port, and either password or SSH key credentials.

## Installation

### 1. Unzip the distribution

Extract `ParseBot.zip` to a permanent location on the target machine (e.g., `C:\ParseBot`). The folder contains everything needed to run -- no separate Java installation is required.

```
ParseBot/
├── installer.exe    # Interactive setup wizard
├── service.exe      # The ParseBot service
├── app/             # Application files
└── runtime/         # Bundled Java runtime
```

### 2. Run the installer

**Right-click `installer.exe` and select "Run as administrator".**

A configuration dialog will appear with fields for every parameter described below. Fill in the required values and click **Install**.

Once the dialog closes, ParseBot is registered as a Windows service, started immediately, and set to start automatically on boot.

### 3. Verify

In the Services application, confirm that the status of **ParseBot Service** shows **Running**.

Logs are written to `logs/parsebot.log` in the installation folder.

## Uninstallation

**Right-click `installer.exe` and select "Run as administrator".** A dialog will ask whether you want to Install or Uninstall -- click **Uninstall**. The service will be stopped and removed.

---

## Configuration Parameters

The installer dialog presents the following fields. Each value is passed to the service at startup.

### Scheduling

| Parameter | Default | Description |
|-----------|---------|-------------|
| **Poll Interval (seconds)** | `60` | How often ParseBot checks for new emails to process, in seconds. Lower values mean faster response to incoming mail but higher resource usage. |

### File System Input

These settings apply only if you are using a local directory as an input source instead of (or in addition to) email.

| Parameter | Default | Description |
|-----------|---------|-------------|
| **Input Directory** | `./input` | Absolute or relative path to a local directory that ParseBot scans for incoming files. |
| **Input File Pattern** | `*.txt` | A glob pattern that determines which files in the input directory are picked up for processing (e.g., `*.pdf`, `*.txt`). |

### Email (IMAP)

These settings configure the mailbox that ParseBot monitors for incoming purchase-order emails.

| Parameter | Default | Required | Description |
|-----------|---------|----------|-------------|
| **Mail Host** | *(none)* | Yes | The hostname or IP address of your IMAP mail server (e.g., `imap.gmail.com`, `outlook.office365.com`). |
| **Mail Port** | `993` | | The port number for the IMAP connection. `993` is the standard port for IMAP over SSL/TLS. Change only if your server uses a non-standard port. |
| **Mail Username** | *(none)* | Yes | The login username for the mail account, typically the full email address. |
| **Mail Password** | *(none)* | Yes | The login password or app-specific password for the mail account. This value is masked in the installer dialog. |
| **Mail Folder** | `INBOX` | | The IMAP folder to monitor for new messages. Most setups should leave this as `INBOX`. |
| **Mail Success Folder** | `Processed` | | The IMAP folder where emails are moved after successful processing. Created automatically if it does not exist. |
| **Mail Failed Folder** | `Failed` | | The IMAP folder where emails are moved when processing fails. Created automatically if it does not exist. |
| **Mail Protocol** | `imaps` | | The mail protocol to use. `imaps` is IMAP over SSL/TLS and is strongly recommended. Use `imap` only if your server does not support SSL. |

### Claude API

| Parameter | Default | Required | Description |
|-----------|---------|----------|-------------|
| **Claude API Key** | *(none)* | Yes | Your Anthropic API key. Used to authenticate requests to the Claude Messages API. This value is masked in the installer dialog. |

### Reference Data

ParseBot sends these CSV files to Claude alongside each PDF so it can match customers and products.

| Parameter | Default | Required | Description |
|-----------|---------|----------|-------------|
| **Customers CSV Path** | *(none)* | Yes | Absolute path to the customer reference CSV file. Claude uses this to match the buyer on each purchase order to a known customer record. |
| **Products CSV Path** | *(none)* | Yes | Absolute path to the product reference CSV file. Claude uses this to match line items on each purchase order to known product records. |

### SFTP Output

These settings configure the remote server where parsed JSON results are uploaded.

| Parameter | Default | Required | Description |
|-----------|---------|----------|-------------|
| **SFTP Host** | *(none)* | Yes | The hostname or IP address of the SFTP server that will receive the parsed output files. |
| **SFTP Port** | `22` | | The port number for the SFTP connection. `22` is the standard SSH/SFTP port. |
| **SFTP Username** | *(none)* | Yes | The login username for the SFTP server. |
| **SFTP Password** | *(none)* | Conditional | The password for SFTP authentication. Required unless you provide a private key instead. This value is masked in the installer dialog. |
| **SFTP Private Key Path** | *(none)* | Conditional | Absolute path to an SSH private key file for key-based SFTP authentication. If provided, this is used instead of the password. |
| **SFTP Remote Directory** | `/upload` | | The directory on the SFTP server where output JSON files are placed. Must already exist on the server. |

---

## Post-Installation Notes

- **Logs** are written to `logs/parsebot.log` relative to the service's working directory, with daily rotation and 30-day retention.
- **Reconfiguration** requires uninstalling and reinstalling the service, as all configuration is baked into the service registration. Uninstall first (see Advanced section), then re-run the installer.
- **IMAP folders** named in the success and failure settings are created automatically on first use if they do not already exist.
- **SFTP authentication** supports either password or SSH private key. If both are provided, the private key takes precedence.

---

## Advanced (Command Line)

For users who prefer command-line operation, the installer supports the following commands. All commands must be run from an **Administrator** terminal.

### Install

```
installer.exe install
```

To specify a service executable in a different location:

```
installer.exe install --exe C:\path\to\service.exe
```

To preview the resulting service command without registering it:

```
installer.exe install --dry-run
```

### Start the service

```
sc start ParseBot
```

### Check service status

```
installer.exe status
```

### Stop the service

```
sc stop ParseBot
```

### Uninstall

```
installer.exe uninstall
```

This stops the service if it is running and removes it from the Windows service registry.
