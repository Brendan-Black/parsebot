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

	private AppConfig(Properties props) {
		this.generalConfig = new GeneralConfig(props);
		this.mailConfig = new MailConfig(props);
		this.claudeConfig = new ClaudeConfig(props);
		this.referenceDataConfig = new ReferenceDataConfig(props);
		this.sftpConfig = new SftpConfig(props);
		this.eventsConfig = new EventsConfig(props);
		this.notifyConfig = new NotifyConfig(props);
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

	public static class GeneralConfig {

		private final Properties props;

		private GeneralConfig(Properties props) {
			this.props = props;
		}

		public long getPollIntervalSeconds() {
			return Long.parseLong(props.getProperty("poll.interval.seconds", "60"));
		}
	}

	public static class ClaudeConfig {

		private final Properties props;

		private ClaudeConfig(Properties props) {
			this.props = props;
		}

		public String getApiKey() {
			return props.getProperty("claude.api.key", "");
		}
	}

	public static class ReferenceDataConfig {

		private final Properties props;

		private ReferenceDataConfig(Properties props) {
			this.props = props;
		}

		public String getCustomerCsvPath() {
			return props.getProperty("csv.customers", "customers.csv");
		}

		public String getProductCsvPath() {
			return props.getProperty("csv.products", "products.csv");
		}

		public String getPriceMatrixCsvPath() {
			return props.getProperty("csv.pricematrix", "");
		}

		public String getCustomRulesDir() {
			return props.getProperty("custom.rules.dir", "custom_rules");
		}

		public String getCustomProductListsDir() {
			return props.getProperty("custom.productlists.dir", "custom_productlists");
		}
	}

	public static class SftpConfig {

		private final Properties props;

		private SftpConfig(Properties props) {
			this.props = props;
		}

		public String getHost() {
			return props.getProperty("sftp.host", "");
		}

		public int getPort() {
			return Integer.parseInt(props.getProperty("sftp.port", "22"));
		}

		public String getUsername() {
			return props.getProperty("sftp.username", "");
		}

		public String getPassword() {
			return props.getProperty("sftp.password", "");
		}

		public String getPrivateKey() {
			return props.getProperty("sftp.private.key", "");
		}

		public String getRemoteDirectory() {
			return props.getProperty("sftp.remote.directory", "/upload");
		}

		public String getProtocol() {
			return props.getProperty("sftp.remote.protocol", "sftp");
		}
	}

	public static class MailConfig {

		private final Properties props;

		private MailConfig(Properties props) {
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

	public static class EventsConfig {

		private final Properties props;

		private EventsConfig(Properties props) {
			this.props = props;
		}

		public int getConsecutiveFailureThreshold() {
			return Integer.parseInt(props.getProperty("events.consecutive.failure.threshold", "3"));
		}

		public String getReportSchedule() {
			return props.getProperty("events.report.schedule", "daily");
		}

		public String getReportTime() {
			return props.getProperty("events.report.time", "08:00");
		}
	}

	public static class NotifyConfig {

		private final Properties props;

		private NotifyConfig(Properties props) {
			this.props = props;
		}

		// SMTP (non-critical)

		public boolean isSmtpEnabled() {
			return Boolean.parseBoolean(props.getProperty("notify.smtp.enabled", "false"));
		}

		public String getSmtpHost() {
			String host = props.getProperty("notify.smtp.host", "");
			return host.isEmpty() ? props.getProperty("mail.host", "") : host;
		}

		public int getSmtpPort() {
			return Integer.parseInt(props.getProperty("notify.smtp.port", "587"));
		}

		public String getSmtpTo() {
			return props.getProperty("notify.smtp.to", "");
		}

		public String getSmtpToUrgent() {
			return props.getProperty("notify.smtp.to.urgent", "");
		}

		public boolean isSmtpStartTls() {
			return Boolean.parseBoolean(props.getProperty("notify.smtp.starttls", "true"));
		}

		// Teams (critical)

		public boolean isTeamsEnabled() {
			return Boolean.parseBoolean(props.getProperty("notify.teams.enabled", "false"));
		}

		public String getTeamsWebhookUrl() {
			return props.getProperty("notify.teams.webhook.url", "");
		}
	}

}
