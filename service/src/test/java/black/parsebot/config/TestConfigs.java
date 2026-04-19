package black.parsebot.config;

import java.util.Properties;

/**
* Test-only helper for constructing config objects with package-private constructors.
*/
public final class TestConfigs {

  private TestConfigs() {}

  public static EventsConfig eventsConfig(String reportTime) {
    Properties p = new Properties();
    if (reportTime != null) p.setProperty(ConfigKey.EVENTS_REPORT_TIME, reportTime);
    return new EventsConfig(p);
  }
}
