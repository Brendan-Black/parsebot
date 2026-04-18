package black.parsebot.checkevents;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

import black.parsebot.event.Event;
import black.parsebot.event.EventType;

public interface EventsUi {

  void showEmpty(Path logDir);

  void showEvents(List<Event> events, Path logDir);

  void searchEvents(EventType type, ZonedDateTime start, ZonedDateTime end);
}
