package black.parsebot.event;

import black.parsebot.notify.NotificationChannel;
import black.parsebot.notify.NotificationDispatcher;
import black.parsebot.storage.Storage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

	private static class CapturingStorage implements Storage<Event> {
		final List<Event> appended = new ArrayList<>();
		@Override public void append(Event item) { appended.add(item); }
		@Override public List<Event> readAll() { return appended; }
		@Override public List<Event> readLast(int n) { return List.of(); }
	}

	@Test
	void publishAppendsToStorageAndForwardsToDispatcher() {
		CapturingStorage storage = new CapturingStorage();
		List<Event> received = new ArrayList<>();
		NotificationChannel ch = received::add;
		NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(ch), List.of(), storage);
		EventBus bus = new EventBus(storage, dispatcher);

		Event e = Event.create(EventType.CONSECUTIVE_FAILURES, EventSeverity.CRITICAL, "m", Map.of());
		bus.publish(e);

		assertEquals(1, storage.appended.size());
		assertEquals(e.id(), storage.appended.get(0).id());
		assertEquals(1, received.size());
		assertEquals(e.id(), received.get(0).id());
	}

	@Test
	void publishSwallowsDispatcherExceptionsAndPersistsNotificationFailed() {
		CapturingStorage storage = new CapturingStorage();
		NotificationDispatcher throwing = new NotificationDispatcher(List.of(), List.of(), storage) {
			@Override
			public void dispatch(Event event) {
				throw new RuntimeException("boom");
			}
		};
		EventBus bus = new EventBus(storage, throwing);

		Event e = Event.create(EventType.REPORT_CARD, EventSeverity.INFO, "m", Map.of());
		assertDoesNotThrow(() -> bus.publish(e));

		assertEquals(2, storage.appended.size());
		assertEquals(e.id(), storage.appended.get(0).id());
		Event failed = storage.appended.get(1);
		assertEquals(EventType.NOTIFICATION_FAILED, failed.type());
		assertEquals(EventSeverity.AUDIT, failed.severity());
		assertEquals(e.id(), failed.details().get("originalEventId"));
	}
}
