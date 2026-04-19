package black.parsebot.config;

import java.util.Properties;

public final class GeneralConfig {

  private final Properties props;

  GeneralConfig(Properties props) {
    this.props = props;
  }

  public long getPollIntervalSeconds() {
    return Long.parseLong(props.getProperty(ConfigKey.POLL_INTERVAL_SECONDS, "60"));
  }
}
