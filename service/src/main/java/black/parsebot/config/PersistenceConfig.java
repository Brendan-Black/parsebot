package black.parsebot.config;

import java.nio.file.Path;
import java.util.Properties;

public final class PersistenceConfig {

  private final Properties props;

  PersistenceConfig(Properties props) {
    this.props = props;
  }

  public Path getStateDir() {
    return Path.of(props.getProperty(ConfigKey.PERSISTENCE_STATE_DIR, "state"));
  }

  public int getHistoryPerEmail() {
    return Integer.parseInt(props.getProperty(ConfigKey.PERSISTENCE_HISTORY_PER_EMAIL, "10"));
  }
}
