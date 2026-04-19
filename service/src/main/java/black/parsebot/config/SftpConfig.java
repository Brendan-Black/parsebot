package black.parsebot.config;

import java.util.Properties;

public final class SftpConfig {

  private final Properties props;

  SftpConfig(Properties props) {
    this.props = props;
  }

  public String getHost() {
    return props.getProperty(ConfigKey.SFTP_HOST, "");
  }

  public int getPort() {
    return Integer.parseInt(props.getProperty(ConfigKey.SFTP_PORT, "22"));
  }

  public String getUsername() {
    return props.getProperty(ConfigKey.SFTP_USERNAME, "");
  }

  public String getPassword() {
    return props.getProperty(ConfigKey.SFTP_PASSWORD, "");
  }

  public String getPrivateKey() {
    return props.getProperty(ConfigKey.SFTP_PRIVATE_KEY, "");
  }

  public String getRemoteDirectory() {
    return props.getProperty(ConfigKey.SFTP_REMOTE_DIRECTORY, "/upload");
  }

  public String getProtocol() {
    return props.getProperty(ConfigKey.SFTP_REMOTE_PROTOCOL, "sftp");
  }
}
