package black.parsebot.notify;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.config.AppConfig;
import black.parsebot.event.Event;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class SmtpChannel implements NotificationChannel {

	private static final Logger log = LoggerFactory.getLogger(SmtpChannel.class);

	private final AppConfig.NotifyConfig notifyConfig;
	private final AppConfig.MailConfig mailConfig;
	private final String recipients;

	public SmtpChannel(AppConfig.NotifyConfig notifyConfig, AppConfig.MailConfig mailConfig, String recipients) {
		this.notifyConfig = notifyConfig;
		this.mailConfig = mailConfig;
		this.recipients = recipients;
	}

	@Override
	public void send(Event event) throws Exception {
		String username = mailConfig.getUsername();
		String password = mailConfig.getPassword();

		Properties props = new Properties();
		props.put("mail.smtp.host", notifyConfig.getSmtpHost());
		props.put("mail.smtp.port", String.valueOf(notifyConfig.getSmtpPort()));
		props.put("mail.smtp.auth", "true");

		if (notifyConfig.isSmtpStartTls()) {
			props.put("mail.smtp.starttls.enable", "true");
		}

		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(username));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
		message.setSubject("ParseBot [" + event.severity() + "] " + event.type());

		StringBuilder body = new StringBuilder();
		body.append("Event: ").append(event.type()).append("\n");
		body.append("Severity: ").append(event.severity()).append("\n");
		body.append("Time: ").append(event.timestamp()).append("\n");
		body.append("Message: ").append(event.message()).append("\n");
		if (event.details() != null && !event.details().isEmpty()) {
			body.append("\nDetails:\n");
			event.details().forEach((k, v) -> body.append("  ").append(k).append(": ").append(v).append("\n"));
		}
		message.setText(body.toString());

		Transport.send(message);
		log.info("Sent email notification for event {} to {}", event.id(), recipients);
	}
}
