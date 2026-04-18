package black.parsebot.checkevents;

import java.nio.file.Path;
import java.util.List;

import black.parsebot.event.Event;

public interface EventsUi {

    void showEmpty(Path logDir);

    void showEvents(List<Event> events, Path logDir);
}
