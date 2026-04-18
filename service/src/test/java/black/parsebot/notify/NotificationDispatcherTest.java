package black.parsebot.notify;

import black.parsebot.event.Event;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDispatcherTest {

	private static class RecordingChannel implements NotificationChannel {
		final List<Event> received = new ArrayList<>();
		@Override public void send(Event event) { received.add(event); }
	}

	private static class ThrowingChannel implements NotificationChannel {
		int calls = 0;
		@Override public void send(Event event) { calls++; throw new RuntimeException("nope"); }
	}

	private static Event critical() {
		return Event.create(EventType.CONSECUTIVE_FAILURES, EventSeverity.CRITICAL, "m", Map.of());
	}

	private static Event info() {
		return Event.create(EventType.REPORT_CARD, EventSeverity.INFO, "m", Map.of());
	}

	@Test
	void criticalRoutesToCriticalChannelsOnly() {
		RecordingChannel crit = new RecordingChannel();
		RecordingChannel inf = new RecordingChannel();
		NotificationDispatcher d = new NotificationDispatcher(List.of(crit), List.of(inf));

		d.dispatch(critical());

		assertEquals(1, crit.received.size());
		assertTrue(inf.received.isEmpty());
	}

	@Test
	void infoRoutesToInfoChannelsOnly() {
		RecordingChannel crit = new RecordingChannel();
		RecordingChannel inf = new RecordingChannel();
		NotificationDispatcher d = new NotificationDispatcher(List.of(crit), List.of(inf));

		d.dispatch(info());

		assertEquals(1, inf.received.size());
		assertTrue(crit.received.isEmpty());
	}

	@Test
	void allChannelsInTargetListAreInvoked() {
		RecordingChannel a = new RecordingChannel();
		RecordingChannel b = new RecordingChannel();
		NotificationDispatcher d = new NotificationDispatcher(List.of(a, b), List.of());

		d.dispatch(critical());

		assertEquals(1, a.received.size());
		assertEquals(1, b.received.size());
	}

	@Test
	void oneChannelFailureDoesNotStopOthers() {
		ThrowingChannel bad = new ThrowingChannel();
		RecordingChannel good = new RecordingChannel();
		NotificationDispatcher d = new NotificationDispatcher(List.of(bad, good), List.of());

		assertDoesNotThrow(() -> d.dispatch(critical()));

		assertEquals(1, bad.calls);
		assertEquals(1, good.received.size());
	}

	@Test
	void emptyChannelListIsNoOp() {
		NotificationDispatcher d = new NotificationDispatcher(List.of(), List.of());
		assertDoesNotThrow(() -> d.dispatch(critical()));
		assertDoesNotThrow(() -> d.dispatch(info()));
	}
}
