package black.parsebot.config;

import java.util.Properties;

public final class MailConfig {

  private final Properties props;

  MailConfig(Properties props) {
    this.props = props;
  }

  public String getHost() {
    return props.getProperty("mail.host", "");
  }

  public int getPort() {
    return Integer.parseInt(props.getProperty("mail.port", "993"));
  }

  public String getUsername() {
    return props.getProperty("mail.username", "");
  }

  public String getPassword() {
    return props.getProperty("mail.password", "");
  }

  public String getFolder() {
    return props.getProperty("mail.folder", "INBOX");
  }

  public String getSuccessFolder() {
    return props.getProperty("mail.folder.success", "Processed");
  }

  public String getFailureFolder() {
    return props.getProperty("mail.folder.failed", "Failed");
  }

  public String getProtocol() {
    return props.getProperty("mail.protocol", "imaps");
  }
}
