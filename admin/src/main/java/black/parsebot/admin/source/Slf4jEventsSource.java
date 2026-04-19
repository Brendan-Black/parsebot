package black.parsebot.admin.source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import black.parsebot.admin.api.EventsApi.EventsResponse;
import black.parsebot.event.Event;
import black.parsebot.storage.Slf4jStorage;
import black.parsebot.storage.Storage;

public final class Slf4jEventsSource implements EventsSource {

  private final Path logDir;

  public Slf4jEventsSource(Path logDir) {
    this.logDir = logDir;
  }

  @Override
  public EventsResponse load() {
    if (!Files.isDirectory(logDir)) {
      return new EventsResponse(logDir.toString(), Collections.emptyList());
    }
    Storage<Event> storage = new Slf4jStorage<>(Event.class, "EVENT: ", logDir, "parsebot*.log");
    try {
      List<Event> events = storage.readAll();
      Collections.reverse(events);
      return new EventsResponse(logDir.toString(), events);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }
}
