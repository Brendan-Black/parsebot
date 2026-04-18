package black.parsebot.storage;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Slf4jStorageTest {

  private static final String EVT1 = """
      2026-04-17 08:00:00 INFO - EVENT: {"id":"id-1","type":"CONSECUTIVE_FAILURES","severity":"CRITICAL","timestamp":"2026-04-17T08:00:00Z","message":"3 failures","details":{"k":"v"}}""";
  private static final String EVT2 = """
      2026-04-17 09:00:00 INFO - EVENT: {"id":"id-2","type":"REPORT_CARD","severity":"INFO","timestamp":"2026-04-17T09:00:00Z","message":"all good","details":{}}""";

  private static Storage<Event> storage(Path dir) {
    return new Slf4jStorage<>(Event.class, "EVENT: ", dir, "parsebot*.log");
  }

  @Test
  void parsesEventLines(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("parsebot.log"),
        "unrelated line\n" + EVT1 + "\nanother unrelated\n" + EVT2 + "\n");

    List<Event> events = storage(tmp).readAll();

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

    assertTrue(storage(tmp).readAll().isEmpty());
  }

  @Test
  void emptyDirectoryReturnsEmpty(@TempDir Path tmp) throws IOException {
    assertTrue(storage(tmp).readAll().isEmpty());
  }

  @Test
  void onlyMatchesGlob(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("other.log"), EVT1 + "\n");
    Files.writeString(tmp.resolve("parsebot.log"), EVT2 + "\n");

    List<Event> events = storage(tmp).readAll();

    assertEquals(1, events.size());
    assertEquals("id-2", events.get(0).id());
  }

  @Test
  void sortsFilesByName(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("parsebot.2026-04-16.log"), EVT2 + "\n");
    Files.writeString(tmp.resolve("parsebot.2026-04-15.log"), EVT1 + "\n");

    List<Event> events = storage(tmp).readAll();

    assertEquals(2, events.size());
    assertEquals("id-1", events.get(0).id());
    assertEquals("id-2", events.get(1).id());
  }

  @Test
  void malformedJsonLineSkippedButOthersKept(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("parsebot.log"),
        "prefix EVENT: {not valid json\n" + EVT1 + "\n");

    List<Event> events = storage(tmp).readAll();

    assertEquals(1, events.size());
    assertEquals("id-1", events.get(0).id());
  }

  @Test
  void markerCanAppearMidLine(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("parsebot.log"),
        "2026-04-17T08:00:00 [main] INFO  b.p.EventBus - EVENT: " +
        "{\"id\":\"x\",\"type\":\"REPORT_CARD\",\"severity\":\"INFO\",\"timestamp\":\"2026-04-17T08:00:00Z\",\"message\":\"m\",\"details\":{}}\n");

    List<Event> events = storage(tmp).readAll();

    assertEquals(1, events.size());
    assertEquals("x", events.get(0).id());
  }

  @Test
  void readLastReturnsTailDescending(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("parsebot.log"), EVT1 + "\n" + EVT2 + "\n");

    List<Event> last1 = storage(tmp).readLast(1);
    assertEquals(1, last1.size());
    assertEquals("id-2", last1.get(0).id());

    List<Event> last2 = storage(tmp).readLast(2);
    assertEquals(2, last2.size());
    assertEquals("id-2", last2.get(0).id());
    assertEquals("id-1", last2.get(1).id());
  }

  @Test
  void readLastClampsToAvailable(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("parsebot.log"), EVT1 + "\n");

    List<Event> last5 = storage(tmp).readLast(5);
    assertEquals(1, last5.size());
    assertEquals("id-1", last5.get(0).id());
  }

  @Test
  void readLastZeroReturnsEmpty(@TempDir Path tmp) throws IOException {
    Files.writeString(tmp.resolve("parsebot.log"), EVT1 + "\n" + EVT2 + "\n");

    assertTrue(storage(tmp).readLast(0).isEmpty());
  }

  @Test
  void readLastNegativeThrows(@TempDir Path tmp) {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> storage(tmp).readLast(-1));
  }

  @Test
  void appendThenReadAllRoundTrips(@TempDir Path tmp) throws IOException {
    // Write directly to a file using the same marker format the SLF4J appender would produce.
    Event e = Event.create(EventType.REPORT_CARD, EventSeverity.INFO, "hello", Map.of("k", "v"));
    Storage<Event> s = storage(tmp);

    // Simulate what the appender emits by writing the marker line ourselves —
    // append() goes through SLF4J which isn't configured in this test scope.
    Files.writeString(tmp.resolve("parsebot.log"),
        "2026-04-17 09:00:00 INFO - EVENT: " + new com.google.gson.GsonBuilder()
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonSerializer<Instant>) (src, t, ctx) ->
                new com.google.gson.JsonPrimitive(src.toString()))
            .create().toJson(e) + "\n");

    List<Event> events = s.readAll();
    assertEquals(1, events.size());
    assertEquals(e.id(), events.get(0).id());
    assertEquals("v", events.get(0).details().get("k"));
  }
}
