package black.parsebot.config;

import java.util.Properties;

public final class MailConfig {

  private final Properties props;

  public MailConfig(Properties props) {
    this.props = props;
  }

  public String getHost() {
    return props.getProperty(ConfigKey.MAIL_HOST, "");
  }

  public int getPort() {
    return Integer.parseInt(props.getProperty(ConfigKey.MAIL_PORT, "993"));
  }

  public String getUsername() {
    return props.getProperty(ConfigKey.MAIL_USERNAME, "");
  }

  public String getPassword() {
    return props.getProperty(ConfigKey.MAIL_PASSWORD, "");
  }

  public String getFolder() {
    return props.getProperty(ConfigKey.MAIL_FOLDER, "INBOX");
  }

  public String getSuccessFolder() {
    return props.getProperty(ConfigKey.MAIL_FOLDER_SUCCESS, "Processed");
  }

  public String getFailureFolder() {
    return props.getProperty(ConfigKey.MAIL_FOLDER_FAILED, "Failed");
  }

  public String getProtocol() {
    return props.getProperty(ConfigKey.MAIL_PROTOCOL, "imaps");
  }
}
