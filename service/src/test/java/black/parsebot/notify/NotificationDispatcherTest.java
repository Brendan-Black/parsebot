package black.parsebot.notify;

import black.parsebot.event.Event;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import black.parsebot.storage.Storage;
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

  private static class CapturingStorage implements Storage<Event> {
    final List<Event> appended = new ArrayList<>();
    @Override public void append(Event item) { appended.add(item); }
    @Override public List<Event> readAll() { return appended; }
    @Override public List<Event> readLast(int n) { return List.of(); }
  }

  private static Event critical() {
    return Event.create(EventType.CONSECUTIVE_FAILURES, EventSeverity.CRITICAL, "m", Map.of());
  }

  private static Event info() {
    return Event.create(EventType.REPORT_CARD, EventSeverity.INFO, "m", Map.of());
  }

  private static Event audit() {
    return Event.create(EventType.PIPELINE_RUN_FAILED, EventSeverity.AUDIT, "m", Map.of());
  }

  @Test
  void criticalRoutesToCriticalChannelsOnly() {
    RecordingChannel crit = new RecordingChannel();
    RecordingChannel inf = new RecordingChannel();
    NotificationDispatcher d = new NotificationDispatcher(List.of(crit), List.of(inf), new CapturingStorage());

    d.dispatch(critical());

    assertEquals(1, crit.received.size());
    assertTrue(inf.received.isEmpty());
  }

  @Test
  void infoRoutesToInfoChannelsOnly() {
    RecordingChannel crit = new RecordingChannel();
    RecordingChannel inf = new RecordingChannel();
    NotificationDispatcher d = new NotificationDispatcher(List.of(crit), List.of(inf), new CapturingStorage());

    d.dispatch(info());

    assertEquals(1, inf.received.size());
    assertTrue(crit.received.isEmpty());
  }

  @Test
  void auditRoutesToNoChannels() {
    RecordingChannel crit = new RecordingChannel();
    RecordingChannel inf = new RecordingChannel();
    NotificationDispatcher d = new NotificationDispatcher(List.of(crit), List.of(inf), new CapturingStorage());

    d.dispatch(audit());

    assertTrue(crit.received.isEmpty());
    assertTrue(inf.received.isEmpty());
  }

  @Test
  void allChannelsInTargetListAreInvoked() {
    RecordingChannel a = new RecordingChannel();
    RecordingChannel b = new RecordingChannel();
    NotificationDispatcher d = new NotificationDispatcher(List.of(a, b), List.of(), new CapturingStorage());

    d.dispatch(critical());

    assertEquals(1, a.received.size());
    assertEquals(1, b.received.size());
  }

  @Test
  void oneChannelFailureDoesNotStopOthers() {
    ThrowingChannel bad = new ThrowingChannel();
    RecordingChannel good = new RecordingChannel();
    CapturingStorage storage = new CapturingStorage();
    NotificationDispatcher d = new NotificationDispatcher(List.of(bad, good), List.of(), storage);

    assertDoesNotThrow(() -> d.dispatch(critical()));

    assertEquals(1, bad.calls);
    assertEquals(1, good.received.size());
  }

  @Test
  void channelFailurePersistsNotificationFailedEvent() {
    ThrowingChannel bad = new ThrowingChannel();
    CapturingStorage storage = new CapturingStorage();
    NotificationDispatcher d = new NotificationDispatcher(List.of(bad), List.of(), storage);

    Event original = critical();
    d.dispatch(original);

    assertEquals(1, storage.appended.size());
    Event failed = storage.appended.get(0);
    assertEquals(EventType.NOTIFICATION_FAILED, failed.type());
    assertEquals(EventSeverity.AUDIT, failed.severity());
    assertEquals(original.id(), failed.details().get("originalEventId"));
    assertEquals("ThrowingChannel", failed.details().get("channel"));
  }

  @Test
  void emptyChannelListIsNoOp() {
    NotificationDispatcher d = new NotificationDispatcher(List.of(), List.of(), new CapturingStorage());
    assertDoesNotThrow(() -> d.dispatch(critical()));
    assertDoesNotThrow(() -> d.dispatch(info()));
  }
}
