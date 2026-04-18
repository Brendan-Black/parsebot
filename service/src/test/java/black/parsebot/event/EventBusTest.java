package black.parsebot.event;

import black.parsebot.notify.NotificationChannel;
import black.parsebot.notify.NotificationDispatcher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EventBusTest {

	@Test
	void publishForwardsToDispatcher() {
		List<Event> received = new ArrayList<>();
		NotificationChannel ch = received::add;
		NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(ch), List.of());
		EventBus bus = new EventBus(dispatcher);

		Event e = Event.create(EventType.CONSECUTIVE_FAILURES, EventSeverity.CRITICAL, "m", Map.of());
		bus.publish(e);

		assertEquals(1, received.size());
		assertEquals(e.id(), received.get(0).id());
	}

	@Test
	void publishSwallowsDispatcherExceptions() {
		// Build a dispatcher whose channel always throws — wrap in a subclass
		// of NotificationDispatcher that re-throws from dispatch() itself.
		NotificationDispatcher throwing = new NotificationDispatcher(List.of(), List.of()) {
			@Override
			public void dispatch(Event event) {
				throw new RuntimeException("boom");
			}
		};
		EventBus bus = new EventBus(throwing);

		Event e = Event.create(EventType.REPORT_CARD, EventSeverity.INFO, "m", Map.of());
		assertDoesNotThrow(() -> bus.publish(e));
	}
}
