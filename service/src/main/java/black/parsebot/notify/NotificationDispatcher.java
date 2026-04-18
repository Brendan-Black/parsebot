package black.parsebot.notify;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.event.Event;

public class NotificationDispatcher {

	private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

	private final List<NotificationChannel> criticalChannels;
	private final List<NotificationChannel> infoChannels;

	public NotificationDispatcher(List<NotificationChannel> criticalChannels, List<NotificationChannel> infoChannels) {
		this.criticalChannels = criticalChannels;
		this.infoChannels = infoChannels;
	}

	public void dispatch(Event event) {
		List<NotificationChannel> targets = switch (event.severity()) {
			case CRITICAL -> criticalChannels;
			case INFO -> infoChannels;
		};

		for (NotificationChannel channel : targets) {
			try {
				channel.send(event);
			} catch (Exception e) {
				log.error("Notification channel {} failed for event {}",
						channel.getClass().getSimpleName(), event.id(), e);
			}
		}
	}
}
