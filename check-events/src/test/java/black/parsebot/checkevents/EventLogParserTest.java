package black.parsebot.checkevents;

import black.parsebot.event.Event;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventLogParserTest {

	private static final String EVT1 = """
			2026-04-17 08:00:00 INFO - EVENT: {"id":"id-1","type":"CONSECUTIVE_FAILURES","severity":"CRITICAL","timestamp":"2026-04-17T08:00:00Z","message":"3 failures","details":{"k":"v"}}""";
	private static final String EVT2 = """
			2026-04-17 09:00:00 INFO - EVENT: {"id":"id-2","type":"REPORT_CARD","severity":"INFO","timestamp":"2026-04-17T09:00:00Z","message":"all good","details":{}}""";

	@Test
	void parsesEventLines(@TempDir Path tmp) throws IOException {
		Files.writeString(tmp.resolve("parsebot.log"),
				"unrelated line\n" + EVT1 + "\nanother unrelated\n" + EVT2 + "\n");

		List<Event> events = EventLogParser.parseEventsFromLogs(tmp);

		assertEquals(2, events.size());
		assertEquals("id-1", events.get(0).id());
		assertEquals(EventType.CONSECUTIVE_FAILURES, events.get(0).type());
		assertEquals(EventSeverity.CRITICAL, events.get(0).severity());
		assertEquals(Instant.parse("2026-04-17T08:00:00Z"), events.get(0).timestamp());
		assertEquals("3 failures", events.get(0).message());
		assertEquals("v", events.get(0).details().get("k"));

		assertEquals("id-2", events.get(1).id());
		assertEquals(EventType.REPORT_CARD, events.get(1).type());
		assertEquals(EventSeverity.INFO, events.get(1).severity());
	}

	@Test
	void ignoresLinesWithoutMarker(@TempDir Path tmp) throws IOException {
		Files.writeString(tmp.resolve("parsebot.log"),
				"boring line\nanother line\nstill nothing\n");

		assertTrue(EventLogParser.parseEventsFromLogs(tmp).isEmpty());
	}

	@Test
	void emptyDirectoryReturnsEmpty(@TempDir Path tmp) throws IOException {
		assertTrue(EventLogParser.parseEventsFromLogs(tmp).isEmpty());
	}

	@Test
	void onlyMatchesParsebotGlob(@TempDir Path tmp) throws IOException {
		// An EVENT: line in a non-parsebot log file should be ignored.
		Files.writeString(tmp.resolve("other.log"), EVT1 + "\n");
		Files.writeString(tmp.resolve("parsebot.log"), EVT2 + "\n");

		List<Event> events = EventLogParser.parseEventsFromLogs(tmp);

		assertEquals(1, events.size());
		assertEquals("id-2", events.get(0).id());
	}

	@Test
	void sortsFilesByName(@TempDir Path tmp) throws IOException {
		// Rotated logs follow logback's date-based naming: parsebot.YYYY-MM-DD.log
		// Lex-sort places rotated logs before the current "parsebot.log" since '.' < 'l'.
		Files.writeString(tmp.resolve("parsebot.2026-04-16.log"), EVT2 + "\n");
		Files.writeString(tmp.resolve("parsebot.2026-04-15.log"), EVT1 + "\n");

		List<Event> events = EventLogParser.parseEventsFromLogs(tmp);

		assertEquals(2, events.size());
		assertEquals("id-1", events.get(0).id());
		assertEquals("id-2", events.get(1).id());
	}

	@Test
	void malformedJsonLineSkippedButOthersKept(@TempDir Path tmp) throws IOException {
		Files.writeString(tmp.resolve("parsebot.log"),
				"prefix EVENT: {not valid json\n" + EVT1 + "\n");

		List<Event> events = EventLogParser.parseEventsFromLogs(tmp);

		assertEquals(1, events.size());
		assertEquals("id-1", events.get(0).id());
	}

	@Test
	void eventMarkerCanAppearMidLine(@TempDir Path tmp) throws IOException {
		// The marker is located by indexOf, not line-start, so timestamp/level prefixes are fine.
		Files.writeString(tmp.resolve("parsebot.log"),
				"2026-04-17T08:00:00 [main] INFO  b.p.EventBus - EVENT: " +
				"{\"id\":\"x\",\"type\":\"REPORT_CARD\",\"severity\":\"INFO\",\"timestamp\":\"2026-04-17T08:00:00Z\",\"message\":\"m\",\"details\":{}}\n");

		List<Event> events = EventLogParser.parseEventsFromLogs(tmp);

		assertEquals(1, events.size());
		assertEquals("x", events.get(0).id());
	}
}
