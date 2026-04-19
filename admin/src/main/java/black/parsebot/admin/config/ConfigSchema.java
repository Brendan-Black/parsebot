package black.parsebot.admin.config;

import java.util.List;

public final class ConfigSchema {

  public static final String SERVICE_NAME = "ParseBot";
  public static final String DISPLAY_NAME = "ParseBot Service";
  public static final String DESCRIPTION = "ParseBot email parsing service";
  public static final String SERVICE_EXE = "service.exe";

  public record Field(String key, String label, String defaultValue, String type) {}

  public record Section(String title, List<Field> fields, String hint) {}

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
          new Field("poll.interval.seconds", "Poll Interval (seconds)", "60", "text")
      ), null),
      new Section("Mail (IMAP)", List.of(
          new Field("mail.host",           "Host",           "",          "text"),
          new Field("mail.port",           "Port",           "993",       "text"),
          new Field("mail.username",       "Username",       "",          "text"),
          new Field("mail.password",       "Password",       "",          "password"),
          new Field("mail.folder",         "Folder",         "INBOX",     "text"),
          new Field("mail.folder.success", "Success Folder", "Processed", "text"),
          new Field("mail.folder.failed",  "Failed Folder",  "Failed",    "text"),
          new Field("mail.protocol",       "Protocol",       "imaps",     "text")
      ), null),
      new Section("Claude API", List.of(
          new Field("claude.api.key", "API Key", "", "password")
      ), null),
      new Section("Reference Data", List.of(
          new Field("csv.customers",           "Customers CSV Path",             "", "file"),
          new Field("csv.products",            "Products CSV Path",              "", "file"),
          new Field("csv.pricematrix",         "Price Matrix CSV Path",          "", "file"),
          new Field("custom.rules.dir",        "Custom Rules Directory",         "", "folder"),
          new Field("custom.productlists.dir", "Custom Product Lists Directory", "", "folder")
      ), null),
      new Section("SFTP Output", List.of(
          new Field("sftp.host",             "Host",             "",        "text"),
          new Field("sftp.port",             "Port",             "22",      "text"),
          new Field("sftp.username",         "Username",         "",        "text"),
          new Field("sftp.password",         "Password",         "",        "password"),
          new Field("sftp.private.key",      "Private Key Path", "",        "file"),
          new Field("sftp.remote.directory", "Remote Directory", "/upload", "text")
      ), null),
      new Section("Events", List.of(
          new Field("events.consecutive.failure.threshold", "Consecutive Failure Threshold", "3",     "text"),
          new Field("events.report.time",                   "Report Time",                   "08:00", "time")
      ), null),
      new Section("Notifications - Email (SMTP)", List.of(
          new Field("notify.smtp.enabled",   "Enabled",             "false", "boolean"),
          new Field("notify.smtp.to",        "To Addresses",        "",      "list"),
          new Field("notify.smtp.to.urgent", "Urgent To Addresses", "",      "list"),
          new Field("notify.smtp.starttls",  "STARTTLS",            "true",  "boolean")
      ), "sms-carriers"),
      new Section("Notifications - Teams", List.of(
          new Field("notify.teams.enabled",     "Enabled",     "false", "boolean"),
          new Field("notify.teams.webhook.url", "Webhook URL", "",      "text")
      ), null)
  );

  private ConfigSchema() {}
}
