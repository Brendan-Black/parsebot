package black.parsebot.event;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.notify.NotificationDispatcher;
import black.parsebot.storage.Storage;

public class EventBus {

	private static final Logger log = LoggerFactory.getLogger(EventBus.class);

	private final Storage<Event> storage;
	private final NotificationDispatcher dispatcher;

	public EventBus(Storage<Event> storage, NotificationDispatcher dispatcher) {
		this.storage = storage;
		this.dispatcher = dispatcher;
	}

	public void publish(Event event) {
		storage.append(event);

		try {
			dispatcher.dispatch(event);
		} catch (Exception e) {
			log.error("Failed to dispatch notification for event {}", event.id(), e);
			// Persist directly — re-publishing through the bus would re-enter dispatch()
			storage.append(Event.create(
					EventType.NOTIFICATION_FAILED,
					EventSeverity.AUDIT,
					"Dispatcher failed for event " + event.id(),
					Map.of(
							"originalEventId", event.id(),
							"originalEventType", String.valueOf(event.type()),
							"error", String.valueOf(e.getMessage())
					)));
		}
	}
}
