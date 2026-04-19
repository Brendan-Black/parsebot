package black.parsebot.admin.source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import black.parsebot.admin.api.EventsApi.EventsResponse;
import black.parsebot.event.Event;
import black.parsebot.persistence.PseudoPersistence;
import black.parsebot.persistence.Slf4jPseudoPersistence;

public final class Slf4jPseudoEventsSource implements EventsSource {

  private final Path logDir;

  public Slf4jPseudoEventsSource(Path logDir) {
    this.logDir = logDir;
  }

  @Override
  public EventsResponse load() {
    if (!Files.isDirectory(logDir)) {
      return new EventsResponse(logDir.toString(), Collections.emptyList());
    }
    PseudoPersistence<Event> storage = new Slf4jPseudoPersistence<>(Event.class, "EVENT: ", logDir, "parsebot*.log");
    try {
      List<Event> events = storage.readAll();
      Collections.reverse(events);
      return new EventsResponse(logDir.toString(), events);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }
}
