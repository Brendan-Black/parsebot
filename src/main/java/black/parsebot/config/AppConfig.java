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

    private AppConfig(Properties props) {
        this.props = props;
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

        return new AppConfig(props);
    }

    public long getPollIntervalSeconds() {
        return Long.parseLong(props.getProperty("poll.interval.seconds", "60"));
    }

    // File system reader settings
    public String getInputDirectory() {
        return props.getProperty("input.directory", "./input");
    }

    public String getFilePattern() {
        return props.getProperty("input.file.pattern", "*.txt");
    }

    // Mail settings
    public String getMailHost() {
        return props.getProperty("mail.host", "");
    }

    public int getMailPort() {
        return Integer.parseInt(props.getProperty("mail.port", "993"));
    }

    public String getMailUsername() {
        return props.getProperty("mail.username", "");
    }

    public String getMailPassword() {
        return props.getProperty("mail.password", "");
    }

    public String getMailFolder() {
        return props.getProperty("mail.folder", "INBOX");
    }

    public String getMailSuccessFolder() {
        return props.getProperty("mail.folder.success", "Processed");
    }

    public String getMailFailureFolder() {
        return props.getProperty("mail.folder.failed", "Failed");
    }

    public String getMailProtocol() {
        return props.getProperty("mail.protocol", "imaps");
    }

    // SFTP settings
    public String getSftpHost() {
        return props.getProperty("sftp.host", "");
    }

    public int getSftpPort() {
        return Integer.parseInt(props.getProperty("sftp.port", "22"));
    }

    public String getSftpUsername() {
        return props.getProperty("sftp.username", "");
    }

    public String getSftpPassword() {
        return props.getProperty("sftp.password", "");
    }

    public String getSftpPrivateKey() {
        return props.getProperty("sftp.private.key", "");
    }

    public String getSftpRemoteDirectory() {
        return props.getProperty("sftp.remote.directory", "/upload");
    }
}
