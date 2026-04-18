package black.parsebot.config;

import java.util.Properties;

public final class SftpConfig {

	private final Properties props;

	SftpConfig(Properties props) {
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
