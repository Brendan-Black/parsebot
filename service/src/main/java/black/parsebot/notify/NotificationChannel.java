package black.parsebot.notify;

import black.parsebot.event.Event;

public interface NotificationChannel {

	void send(Event event) throws Exception;
}
