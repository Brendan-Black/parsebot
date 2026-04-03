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

	private final Properties props;
	private final SftpConfig sftpConfig;
	private final MailConfig mailConfig;
	private final FileSystemConfig fileSystemConfig;

	private AppConfig(Properties props) {
		this.props = props;
		this.sftpConfig = new SftpConfig(props);
		this.mailConfig = new MailConfig(props);
		this.fileSystemConfig = new FileSystemConfig(props);
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

	public long getPollIntervalSeconds() {
		return Long.parseLong(props.getProperty("poll.interval.seconds", "60"));
	}

	public SftpConfig getSftpConfig() {
		return sftpConfig;
	}

	public MailConfig getMailConfig() {
		return mailConfig;
	}

	public FileSystemConfig getFileSystemConfig() {
		return fileSystemConfig;
	}

	public String getClaudeApiKey() {
		return props.getProperty("claude.api.key", "");
	}

	public String getCustomerCsvPath() {
		return props.getProperty("csv.customers", "customers.csv");
	}

	public String getProductCsvPath() {
		return props.getProperty("csv.products", "products.csv");
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

	public static class FileSystemConfig {

		private final Properties props;

		private FileSystemConfig(Properties props) {
			this.props = props;
		}

		public String getCenter() {
			return props.getProperty("input.directory", "./input");
		}

		public String getFilePattern() {
			return props.getProperty("input.file.pattern", "*.txt");
		}
	}
}
