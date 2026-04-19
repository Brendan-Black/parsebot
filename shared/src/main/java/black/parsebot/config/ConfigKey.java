package black.parsebot.config;

public final class ConfigKey {

  public static final String POLL_INTERVAL_SECONDS = "poll.interval.seconds";

  public static final String MAIL_HOST = "mail.host";
  public static final String MAIL_PORT = "mail.port";
  public static final String MAIL_USERNAME = "mail.username";
  public static final String MAIL_PASSWORD = "mail.password";
  public static final String MAIL_FOLDER = "mail.folder";
  public static final String MAIL_FOLDER_SUCCESS = "mail.folder.success";
  public static final String MAIL_FOLDER_FAILED = "mail.folder.failed";
  public static final String MAIL_PROTOCOL = "mail.protocol";

  public static final String CLAUDE_API_KEY = "claude.api.key";

  public static final String CSV_CUSTOMERS = "csv.customers";
  public static final String CSV_PRODUCTS = "csv.products";
  public static final String CSV_PRICEMATRIX = "csv.pricematrix";
  public static final String CUSTOM_RULES_DIR = "custom.rules.dir";
  public static final String CUSTOM_PRODUCTLISTS_DIR = "custom.productlists.dir";

  public static final String SFTP_HOST = "sftp.host";
  public static final String SFTP_PORT = "sftp.port";
  public static final String SFTP_USERNAME = "sftp.username";
  public static final String SFTP_PASSWORD = "sftp.password";
  public static final String SFTP_PRIVATE_KEY = "sftp.private.key";
  public static final String SFTP_REMOTE_DIRECTORY = "sftp.remote.directory";
  public static final String SFTP_REMOTE_PROTOCOL = "sftp.remote.protocol";

  public static final String EVENTS_CONSECUTIVE_FAILURE_THRESHOLD = "events.consecutive.failure.threshold";
  public static final String EVENTS_REPORT_TIME = "events.report.time";

  public static final String NOTIFY_SMTP_ENABLED = "notify.smtp.enabled";
  public static final String NOTIFY_SMTP_HOST = "notify.smtp.host";
  public static final String NOTIFY_SMTP_PORT = "notify.smtp.port";
  public static final String NOTIFY_SMTP_TO = "notify.smtp.to";
  public static final String NOTIFY_SMTP_TO_URGENT = "notify.smtp.to.urgent";
  public static final String NOTIFY_SMTP_STARTTLS = "notify.smtp.starttls";

  public static final String NOTIFY_TEAMS_ENABLED = "notify.teams.enabled";
  public static final String NOTIFY_TEAMS_WEBHOOK_URL = "notify.teams.webhook.url";

  private ConfigKey() {}
}
