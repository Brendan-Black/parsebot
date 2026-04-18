package black.parsebot.event;

import black.parsebot.config.TestConfigs;
import black.parsebot.notify.NotificationDispatcher;
import black.parsebot.storage.Storage;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportCardSchedulerTest {

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
	void emitsReportWhenTimeHasPassed() {
		CapturingBus bus = new CapturingBus();
		AtomicInteger success = new AtomicInteger(7);
		AtomicInteger fail = new AtomicInteger(3);
		ReportCardScheduler s = new ReportCardScheduler(
				TestConfigs.eventsConfig("00:00"), bus, success, fail);

		s.run();

		assertEquals(1, bus.captured.size());
		Event e = bus.captured.get(0);
		assertEquals(EventType.REPORT_CARD, e.type());
		assertEquals(EventSeverity.INFO, e.severity());
		assertEquals("10", e.details().get("totalProcessed"));
		assertEquals("7", e.details().get("successCount"));
		assertEquals("3", e.details().get("failCount"));
	}

	@Test
	void emittingResetsCountersToZero() {
		CapturingBus bus = new CapturingBus();
		AtomicInteger success = new AtomicInteger(5);
		AtomicInteger fail = new AtomicInteger(2);
		ReportCardScheduler s = new ReportCardScheduler(
				TestConfigs.eventsConfig("00:00"), bus, success, fail);

		s.run();

		assertEquals(0, success.get());
		assertEquals(0, fail.get());
	}

	@Test
	void secondRunSameDayIsNoOp() {
		CapturingBus bus = new CapturingBus();
		AtomicInteger success = new AtomicInteger(1);
		AtomicInteger fail = new AtomicInteger(0);
		ReportCardScheduler s = new ReportCardScheduler(
				TestConfigs.eventsConfig("00:00"), bus, success, fail);

		s.run();
		success.set(4);
		fail.set(1);
		s.run();

		assertEquals(1, bus.captured.size());
		// Counters from the second attempt survived because we didn't emit again
		assertEquals(4, success.get());
		assertEquals(1, fail.get());
	}

	@Test
	void doesNothingBeforeReportTime() {
		// Pick a report time guaranteed to be in the future relative to "now".
		// If we're within the last minute of the day, skip rather than flake.
		LocalTime now = LocalTime.now();
		if (now.getHour() == 23 && now.getMinute() >= 58) return;
		String future = LocalTime.of(23, 59).toString().substring(0, 5);

		CapturingBus bus = new CapturingBus();
		AtomicInteger success = new AtomicInteger(3);
		AtomicInteger fail = new AtomicInteger(1);
		ReportCardScheduler s = new ReportCardScheduler(
				TestConfigs.eventsConfig(future), bus, success, fail);

		s.run();

		assertTrue(bus.captured.isEmpty());
		assertEquals(3, success.get());
		assertEquals(1, fail.get());
	}

	@Test
	void zeroTotalsEmitsZeroReport() {
		CapturingBus bus = new CapturingBus();
		ReportCardScheduler s = new ReportCardScheduler(
				TestConfigs.eventsConfig("00:00"), bus, new AtomicInteger(0), new AtomicInteger(0));

		s.run();

		assertEquals(1, bus.captured.size());
		Event e = bus.captured.get(0);
		assertEquals("0", e.details().get("totalProcessed"));
	}
}
