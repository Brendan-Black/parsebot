package black.parsebot.config;

import java.util.Properties;

public final class ClaudeConfig {

	private final Properties props;

	ClaudeConfig(Properties props) {
		this.props = props;
	}

	public String getApiKey() {
		return props.getProperty("claude.api.key", "");
	}
}
