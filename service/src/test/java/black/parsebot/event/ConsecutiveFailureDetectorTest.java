package black.parsebot.event;

import black.parsebot.notify.NotificationDispatcher;
import black.parsebot.storage.Storage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsecutiveFailureDetectorTest {

	private static class NoopStorage implements Storage<Event> {
		@Override public void append(Event item) {}
		@Override public List<Event> readAll() { return List.of(); }
	}

	private static class CapturingBus extends EventBus {
		final List<Event> captured = new ArrayList<>();
		CapturingBus() { super(new NoopStorage(), new NotificationDispatcher(List.of(), List.of())); }
		@Override public void publish(Event event) { captured.add(event); }
	}

	@Test
	void fewerThanThresholdFailuresEmitsNothing() {
		CapturingBus bus = new CapturingBus();
		ConsecutiveFailureDetector d = new ConsecutiveFailureDetector(3, bus);
		d.recordFailure("a", "oops");
		d.recordFailure("b", "oops");
		assertTrue(bus.captured.isEmpty());
	}

	@Test
	void exactlyThresholdFiresEvent() {
		CapturingBus bus = new CapturingBus();
		ConsecutiveFailureDetector d = new ConsecutiveFailureDetector(3, bus);
		d.recordFailure("a", "boom");
		d.recordFailure("b", "boom");
		d.recordFailure("c", "final");
		assertEquals(1, bus.captured.size());
		Event e = bus.captured.get(0);
		assertEquals(EventType.CONSECUTIVE_FAILURES, e.type());
		assertEquals(EventSeverity.CRITICAL, e.severity());
		assertEquals("c", e.details().get("lastFailedEmail"));
		assertEquals("final", e.details().get("errorSummary"));
	}

	@Test
	void successResetsCounter() {
		CapturingBus bus = new CapturingBus();
		ConsecutiveFailureDetector d = new ConsecutiveFailureDetector(3, bus);
		d.recordFailure("a", "x");
		d.recordFailure("b", "x");
		d.recordSuccess();
		d.recordFailure("c", "x");
		d.recordFailure("d", "x");
		assertTrue(bus.captured.isEmpty());
	}

	@Test
	void firesAgainAfterResetOnNextThresholdRun() {
		CapturingBus bus = new CapturingBus();
		ConsecutiveFailureDetector d = new ConsecutiveFailureDetector(2, bus);
		d.recordFailure("a", "x");
		d.recordFailure("b", "x"); // fires, resets to 0
		d.recordFailure("c", "x");
		d.recordFailure("d", "x"); // fires again
		assertEquals(2, bus.captured.size());
	}

	@Test
	void nullEmailAndErrorMessageBecomeUnknown() {
		CapturingBus bus = new CapturingBus();
		ConsecutiveFailureDetector d = new ConsecutiveFailureDetector(1, bus);
		d.recordFailure(null, null);
		Event e = bus.captured.get(0);
		assertEquals("unknown", e.details().get("lastFailedEmail"));
		assertEquals("unknown", e.details().get("errorSummary"));
	}

	@Test
	void thresholdOfOneFiresImmediately() {
		CapturingBus bus = new CapturingBus();
		ConsecutiveFailureDetector d = new ConsecutiveFailureDetector(1, bus);
		d.recordFailure("a", "x");
		assertEquals(1, bus.captured.size());
	}
}
