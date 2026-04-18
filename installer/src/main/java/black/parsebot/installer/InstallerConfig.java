package black.parsebot.installer;

import java.util.List;
import java.util.Set;

final class InstallerConfig {

  static final String SERVICE_NAME = "ParseBot";
  static final String DISPLAY_NAME = "ParseBot Service";
  static final String DESCRIPTION = "ParseBot email parsing service";
  static final String SERVICE_EXE = "service.exe";

  record Field(String key, String label, String defaultValue) {}

  record Section(String title, List<Field> fields, String hint) {
    Section(String title, List<Field> fields) {
      this(title, fields, null);
    }
  }

  static final Set<String> FOLDER_FIELDS = Set.of("custom.rules.dir", "custom.productlists.dir");
  static final Set<String> FILE_FIELDS = Set.of("csv.customers", "csv.products", "csv.pricematrix", "sftp.private.key");
  static final Set<String> BOOLEAN_FIELDS = Set.of("notify.smtp.enabled", "notify.smtp.starttls", "notify.teams.enabled");
  static final Set<String> MULTILINE_FIELDS = Set.of("notify.smtp.to", "notify.smtp.to.urgent");
  static final Set<String> TIME_FIELDS = Set.of("events.report.time");

  static final String[][] SMS_CARRIERS = {
      {"#@vtext.com", "Verizon"},
      {"#@tmomail.net", "T-Mobile"},
      {"#@txt.att.net", "AT&T"},
      {"#@messaging.sprintpcs.com", "Sprint"},
      {"#@msg.fi.google.com", "Google Fi"},
      {"#@message.ting.com", "Ting"},
      {"#@email.uscc.net", "US Cellular"},
      {"#@sms.cricketwireless.net", "Cricket"},
      {"#@myboostmobile.com", "Boost"},
      {"#@text.republicwireless.com", "Republic"},
      {"#@vmobl.com", "Virgin Mobile"},
      {"#@mmst5.tracfone.com", "Tracfone"},
      {"#@mymetropcs.com", "Metro"},
      {"#@sms.mypage.com", "PagePlus"},
      {"#@mailmymobile.net", "Consumer Cellular"},
      {"#@cspire1.com", "C-Spire"},
  };

  static final String SMS_HINT =
      "#@vtext.com (Verizon)\n" +
      "#@tmomail.net (T-Mobile)\n" +
      "#@txt.att.net (AT&T)\n" +
      "#@messaging.sprintpcs.com (Sprint)\n" +
      "#@msg.fi.google.com (Google Fi)\n" +
      "#@message.ting.com (Ting)\n" +
      "#@email.uscc.net (US Cellular)\n" +
      "#@sms.cricketwireless.net (Cricket)\n" +
      "#@myboostmobile.com (Boost)\n" +
      "#@text.republicwireless.com (Republic)\n" +
      "#@vmobl.com (Virgin Mobile)\n" +
      "#@mmst5.tracfone.com (Tracfone)\n" +
      "#@mymetropcs.com (Metro)\n" +
      "#@sms.mypage.com (PagePlus)\n" +
      "#@mailmymobile.net (Consumer Cellular)\n" +
      "#@cspire1.com (C-Spire)";

  static final List<Section> SECTIONS = List.of(
      new Section("General", List.of(
          new Field("poll.interval.seconds", "Poll Interval (seconds)", "60")
      )),
      new Section("Mail (IMAP)", List.of(
          new Field("mail.host",           "Host",           ""),
          new Field("mail.port",           "Port",           "993"),
          new Field("mail.username",       "Username",       ""),
          new Field("mail.password",       "Password",       ""),
          new Field("mail.folder",         "Folder",         "INBOX"),
          new Field("mail.folder.success", "Success Folder", "Processed"),
          new Field("mail.folder.failed",  "Failed Folder",  "Failed"),
          new Field("mail.protocol",       "Protocol",       "imaps")
      )),
      new Section("Claude API", List.of(
          new Field("claude.api.key", "API Key", "")
      )),
      new Section("Reference Data", List.of(
          new Field("csv.customers",          "Customers CSV Path",              ""),
          new Field("csv.products",           "Products CSV Path",               ""),
          new Field("csv.pricematrix",        "Price Matrix CSV Path",           ""),
          new Field("custom.rules.dir",       "Custom Rules Directory",          ""),
          new Field("custom.productlists.dir","Custom Product Lists Directory",  "")
      )),
      new Section("SFTP Output", List.of(
          new Field("sftp.host",             "Host",             ""),
          new Field("sftp.port",             "Port",             "22"),
          new Field("sftp.username",         "Username",         ""),
          new Field("sftp.password",         "Password",         ""),
          new Field("sftp.private.key",      "Private Key Path", ""),
          new Field("sftp.remote.directory", "Remote Directory", "/upload")
      )),
      new Section("Events", List.of(
          new Field("events.consecutive.failure.threshold", "Consecutive Failure Threshold", "3"),
          new Field("events.report.time",                   "Report Time",                   "08:00")
      )),
      new Section("Notifications - Email (SMTP)", List.of(
          new Field("notify.smtp.enabled",   "Enabled (true/false)", "false"),
          new Field("notify.smtp.to",        "To Addresses",         ""),
          new Field("notify.smtp.to.urgent", "Urgent To Addresses",  ""),
          new Field("notify.smtp.starttls",  "STARTTLS (true/false)","true")
      ), SMS_HINT),
      new Section("Notifications - Teams", List.of(
          new Field("notify.teams.enabled",     "Enabled (true/false)", "false"),
          new Field("notify.teams.webhook.url", "Webhook URL",          "")
      ))
  );

  private InstallerConfig() {}
}
