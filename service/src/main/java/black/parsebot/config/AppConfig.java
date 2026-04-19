package black.parsebot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {

  private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
  private static final String CONFIG_FILE = "parsebot.properties";

  private final GeneralConfig generalConfig;
  private final MailConfig mailConfig;
  private final ClaudeConfig claudeConfig;
  private final ReferenceDataConfig referenceDataConfig;
  private final SftpConfig sftpConfig;
  private final EventsConfig eventsConfig;
  private final NotifyConfig notifyConfig;
  private final PersistenceConfig persistenceConfig;

  private AppConfig(Properties props) {
    this.generalConfig = new GeneralConfig(props);
    this.mailConfig = new MailConfig(props);
    this.claudeConfig = new ClaudeConfig(props);
    this.referenceDataConfig = new ReferenceDataConfig(props);
    this.sftpConfig = new SftpConfig(props);
    this.eventsConfig = new EventsConfig(props);
    this.notifyConfig = new NotifyConfig(props);
    this.persistenceConfig = new PersistenceConfig(props);
  }

  public static AppConfig load() {
    Properties props = new Properties();
    Path externalConfig = Path.of(CONFIG_FILE);

    if (Files.exists(externalConfig)) {
      try (InputStream in = Files.newInputStream(externalConfig)) {
        props.load(in);
        log.info("Loaded config from {}", externalConfig.toAbsolutePath());
      } catch (IOException e) {
        throw new RuntimeException("Failed to load config: " + externalConfig, e);
      }
    } else {
      try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
        if (in != null) {
          props.load(in);
          log.info("Loaded config from classpath");
        } else {
          log.warn("No config file found, using defaults");
        }
      } catch (IOException e) {
        throw new RuntimeException("Failed to load classpath config", e);
      }
    }

    // System properties with "parsebot." prefix override file/classpath values.
    // e.g. -Dparsebot.mail.host=imap.example.com overrides mail.host
    for (String key : System.getProperties().stringPropertyNames()) {
      if (key.startsWith("parsebot.")) {
        String configKey = key.substring("parsebot.".length());
        props.setProperty(configKey, System.getProperty(key));
        log.info("Config override from system property: {}", configKey);
      }
    }

    return new AppConfig(props);
  }

  public GeneralConfig getGeneralConfig() {
    return generalConfig;
  }

  public MailConfig getMailConfig() {
    return mailConfig;
  }

  public ClaudeConfig getClaudeConfig() {
    return claudeConfig;
  }

  public ReferenceDataConfig getReferenceDataConfig() {
    return referenceDataConfig;
  }

  public SftpConfig getSftpConfig() {
    return sftpConfig;
  }

  public EventsConfig getEventsConfig() {
    return eventsConfig;
  }

  public NotifyConfig getNotifyConfig() {
    return notifyConfig;
  }

  public PersistenceConfig getPersistenceConfig() {
    return persistenceConfig;
  }
}
