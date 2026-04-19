package black.parsebot.config;

import java.util.Properties;

public final class NotifyConfig {

  private final Properties props;

  NotifyConfig(Properties props) {
    this.props = props;
  }

  // SMTP (non-critical)

  public boolean isSmtpEnabled() {
    return Boolean.parseBoolean(props.getProperty(ConfigKey.NOTIFY_SMTP_ENABLED, "false"));
  }

  public String getSmtpHost() {
    String host = props.getProperty(ConfigKey.NOTIFY_SMTP_HOST, "");
    return host.isEmpty() ? props.getProperty(ConfigKey.MAIL_HOST, "") : host;
  }

  public int getSmtpPort() {
    return Integer.parseInt(props.getProperty(ConfigKey.NOTIFY_SMTP_PORT, "587"));
  }

  public String getSmtpTo() {
    return props.getProperty(ConfigKey.NOTIFY_SMTP_TO, "");
  }

  public String getSmtpToUrgent() {
    return props.getProperty(ConfigKey.NOTIFY_SMTP_TO_URGENT, "");
  }

  public boolean isSmtpStartTls() {
    return Boolean.parseBoolean(props.getProperty(ConfigKey.NOTIFY_SMTP_STARTTLS, "true"));
  }

  // Teams (critical)

  public boolean isTeamsEnabled() {
    return Boolean.parseBoolean(props.getProperty(ConfigKey.NOTIFY_TEAMS_ENABLED, "false"));
  }

  public String getTeamsWebhookUrl() {
    return props.getProperty(ConfigKey.NOTIFY_TEAMS_WEBHOOK_URL, "");
  }
}
