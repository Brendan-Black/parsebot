package black.parsebot.config;

import java.util.Properties;

public final class NotifyConfig {

	private final Properties props;

	NotifyConfig(Properties props) {
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
