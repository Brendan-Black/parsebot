package black.parsebot.config;

import java.util.Properties;

public final class EventsConfig {

  private final Properties props;

  EventsConfig(Properties props) {
    this.props = props;
  }

  public int getConsecutiveFailureThreshold() {
    return Integer.parseInt(props.getProperty("events.consecutive.failure.threshold", "3"));
  }

  public String getReportTime() {
    return props.getProperty("events.report.time", "08:00");
  }
}
