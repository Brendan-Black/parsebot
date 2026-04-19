package black.parsebot.config;

import java.util.Properties;

public final class EventsConfig {

  private final Properties props;

  EventsConfig(Properties props) {
    this.props = props;
  }

  public int getConsecutiveFailureThreshold() {
    return Integer.parseInt(props.getProperty(ConfigKey.EVENTS_CONSECUTIVE_FAILURE_THRESHOLD, "3"));
  }

  public String getReportTime() {
    return props.getProperty(ConfigKey.EVENTS_REPORT_TIME, "08:00");
  }
}
