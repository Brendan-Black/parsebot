# ParseBot — Project Overview & Requirements

Hi all,

Attached you'll find the full product description for ParseBot, an automated email-to-data pipeline that reads incoming orders from a shared mailbox, extracts structured data using AI, and deposits the results for intake by your DDI system.

This document covers what I'll need from your teams to move forward, anticipated costs, and a few operational considerations worth discussing early.

---

## Scope

The attached `PRODUCT_DESCRIPTION.md` defines the full scope of the application. Two items worth calling out that are **not** included:

- **Notifications** — The application does not send alerts via email, text, Slack, Teams, or otherwise.
- **Input validation** — The application does not filter or reject attachments before processing. See [Attachment Filtering](#attachment-filtering) below for why this matters and what I'd recommend.

---

## What I Need

### From your Server Team

- Mail credentials (username / password)
- Mail connection info (protocol / host / port)
- Mailbox configuration\*
- Code signing info (if applicable)\*\*

> \* The simplest setup is three mailboxes: one for incoming mail, one where successfully processed emails are moved, and one for failures. I'm flexible on this — I just need to know how you'd like it arranged.

> \*\* If the server is a Windows environment, we may need a code signing certificate to avoid security warnings during installation. Whether this is necessary depends on your security policies. More on cost below.

### From your DDI Team

- SFTP credentials (username / password)
- SFTP connection info (host / port)
- Target folder(s) for depositing processed data
- Desired output format (JSON / XML / CSV / other)
- Network routing details, if the DDI system is hosted on a separate machine from ParseBot

### From whoever manages product/customer data

- A master product list
- A master customer list

These are reference files ParseBot uses to match and validate extracted data. I need a single, stable location to read from — a file on the server that your team can overwrite as needed. Alternatively, this could point to a shared Google Sheet or Teams file if that's easier to maintain.

---

## Anticipated Costs

### Code Signing Certificate — $120–$1,000/yr

This is a Microsoft requirement for installing software without triggering security warnings. The cost depends on the certificate type (standard vs. EV). **This may not be needed at all** if your environment permits unsigned installs — worth confirming with your server team.

### AI Processing (Anthropic API) — $5–$100/mo

Each document processed consumes API tokens, and cost scales with document size and complexity. A typical single-page order is inexpensive; a multi-page PDF with dense content costs more. I can provide a tighter estimate once I see a representative sample of the orders you receive. Document compression can reduce this, though with some trade-off in accuracy.

---

## Operational Considerations

### Remote Access

Since I won't have direct access to the server, all troubleshooting will depend on your server team forwarding logs to me. I also won't be able to make changes remotely — any restarts, updates, or configuration changes will need to go through your team. We should establish a process for this early on.

### AI Service Dependency

ParseBot relies on Anthropic's Claude API, which maintains 99%+ uptime. That said, any sustained API outage would prevent the application from processing new emails until service is restored. Emails would remain in the inbox and be processed once the service comes back.

### Attachment Filtering

Under the current design, ParseBot processes every attachment on every incoming email. If someone sends a large or irrelevant file — a product manual, a company logo, a 400-page PDF — it will still be read and processed, and the API cost for that document will apply.

I'd recommend we discuss one or more of the following mitigations:

- A maximum file size per attachment
- An allowlist of accepted file types (e.g., PDF only)
- A dedicated sender address or subject-line convention to filter relevant emails

These are straightforward to implement and would prevent unnecessary costs.

---

## Next Steps

Once you've had a chance to review the attached product description, I'd suggest we schedule a brief call to align on the mailbox setup, DDI integration details, and whether code signing is needed. In the meantime, if your teams can start gathering the credentials and connection info listed above, that will keep things moving.

Looking forward to it.

— Brendan
