package black.parsebot.admin.config;

import java.util.List;

import black.parsebot.config.ConfigKey;

public final class ConfigSchema {

  public static final String SERVICE_NAME = "ParseBot";
  public static final String DISPLAY_NAME = "ParseBot Service";
  public static final String DESCRIPTION = "ParseBot email parsing service";
  public static final String SERVICE_EXE = "service.exe";

  public record Field(String key, String label, String defaultValue, FieldType type) {}

  public record Section(String title, List<Field> fields, SectionHint hint) {}

  public record Carrier(String gateway, String carrier) {}

  public static final List<Carrier> SMS_CARRIERS = List.of(
      new Carrier("#@vtext.com", "Verizon"),
      new Carrier("#@tmomail.net", "T-Mobile"),
      new Carrier("#@txt.att.net", "AT&T"),
      new Carrier("#@messaging.sprintpcs.com", "Sprint"),
      new Carrier("#@msg.fi.google.com", "Google Fi"),
      new Carrier("#@message.ting.com", "Ting"),
      new Carrier("#@email.uscc.net", "US Cellular"),
      new Carrier("#@sms.cricketwireless.net", "Cricket"),
      new Carrier("#@myboostmobile.com", "Boost"),
      new Carrier("#@text.republicwireless.com", "Republic"),
      new Carrier("#@vmobl.com", "Virgin Mobile"),
      new Carrier("#@mmst5.tracfone.com", "Tracfone"),
      new Carrier("#@mymetropcs.com", "Metro"),
      new Carrier("#@sms.mypage.com", "PagePlus"),
      new Carrier("#@mailmymobile.net", "Consumer Cellular"),
      new Carrier("#@cspire1.com", "C-Spire")
  );

  public static final List<Section> SECTIONS = List.of(
      new Section("General", List.of(
          new Field(ConfigKey.POLL_INTERVAL_SECONDS, "Poll Interval (seconds)", "60", FieldType.TEXT)
      ), null),
      new Section("Mail (IMAP)", List.of(
          new Field(ConfigKey.MAIL_HOST,           "Host",           "",          FieldType.TEXT),
          new Field(ConfigKey.MAIL_PORT,           "Port",           "993",       FieldType.TEXT),
          new Field(ConfigKey.MAIL_USERNAME,       "Username",       "",          FieldType.TEXT),
          new Field(ConfigKey.MAIL_PASSWORD,       "Password",       "",          FieldType.PASSWORD),
          new Field(ConfigKey.MAIL_FOLDER,         "Folder",         "INBOX",     FieldType.TEXT),
          new Field(ConfigKey.MAIL_FOLDER_SUCCESS, "Success Folder", "Processed", FieldType.TEXT),
          new Field(ConfigKey.MAIL_FOLDER_FAILED,  "Failed Folder",  "Failed",    FieldType.TEXT),
          new Field(ConfigKey.MAIL_PROTOCOL,       "Protocol",       "imaps",     FieldType.TEXT)
      ), null),
      new Section("Claude API", List.of(
          new Field(ConfigKey.CLAUDE_API_KEY, "API Key", "", FieldType.PASSWORD)
      ), null),
      new Section("Reference Data", List.of(
          new Field(ConfigKey.CSV_CUSTOMERS,           "Customers CSV Path",             "", FieldType.FILE),
          new Field(ConfigKey.CSV_PRODUCTS,            "Products CSV Path",              "", FieldType.FILE),
          new Field(ConfigKey.CSV_PRICEMATRIX,         "Price Matrix CSV Path",          "", FieldType.FILE),
          new Field(ConfigKey.CUSTOM_RULES_DIR,        "Custom Rules Directory",         "", FieldType.FOLDER),
          new Field(ConfigKey.CUSTOM_PRODUCTLISTS_DIR, "Custom Product Lists Directory", "", FieldType.FOLDER)
      ), null),
      new Section("SFTP Output", List.of(
          new Field(ConfigKey.SFTP_HOST,             "Host",             "",        FieldType.TEXT),
          new Field(ConfigKey.SFTP_PORT,             "Port",             "22",      FieldType.TEXT),
          new Field(ConfigKey.SFTP_USERNAME,         "Username",         "",        FieldType.TEXT),
          new Field(ConfigKey.SFTP_PASSWORD,         "Password",         "",        FieldType.PASSWORD),
          new Field(ConfigKey.SFTP_PRIVATE_KEY,      "Private Key Path", "",        FieldType.FILE),
          new Field(ConfigKey.SFTP_REMOTE_DIRECTORY, "Remote Directory", "/upload", FieldType.TEXT)
      ), null),
      new Section("Events", List.of(
          new Field(ConfigKey.EVENTS_CONSECUTIVE_FAILURE_THRESHOLD, "Consecutive Failure Threshold", "3",     FieldType.TEXT),
          new Field(ConfigKey.EVENTS_REPORT_TIME,                   "Report Time",                   "08:00", FieldType.TIME)
      ), null),
      new Section("Notifications - Email (SMTP)", List.of(
          new Field(ConfigKey.NOTIFY_SMTP_ENABLED,   "Enabled",             "false", FieldType.BOOLEAN),
          new Field(ConfigKey.NOTIFY_SMTP_TO,        "To Addresses",        "",      FieldType.LIST),
          new Field(ConfigKey.NOTIFY_SMTP_TO_URGENT, "Urgent To Addresses", "",      FieldType.LIST),
          new Field(ConfigKey.NOTIFY_SMTP_STARTTLS,  "STARTTLS",            "true",  FieldType.BOOLEAN)
      ), SectionHint.SMS_CARRIERS),
      new Section("Notifications - Teams", List.of(
          new Field(ConfigKey.NOTIFY_TEAMS_ENABLED,     "Enabled",     "false", FieldType.BOOLEAN),
          new Field(ConfigKey.NOTIFY_TEAMS_WEBHOOK_URL, "Webhook URL", "",      FieldType.TEXT)
      ), null)
  );

  private ConfigSchema() {}
}
