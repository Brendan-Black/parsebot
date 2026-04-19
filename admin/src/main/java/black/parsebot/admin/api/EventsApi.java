package black.parsebot.admin.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.JsonHandler;
import black.parsebot.event.Event;
import black.parsebot.storage.Slf4jStorage;
import black.parsebot.storage.Storage;

public final class EventsApi {

  public record EventsResponse(String logDir, List<Event> events) {}

  public static final class EventsHandler extends JsonHandler {
    private final Path logDir;

    public EventsHandler(Path logDir) {
      this.logDir = logDir;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) throws Exception {
      requireMethod(exchange, "GET");
      if (!Files.isDirectory(logDir)) {
        return new EventsResponse(logDir.toString(), Collections.emptyList());
      }
      Storage<Event> storage = new Slf4jStorage<>(Event.class, "EVENT: ", logDir, "parsebot*.log");
      List<Event> events = storage.readAll();
      Collections.reverse(events);
      return new EventsResponse(logDir.toString(), events);
    }
  }

  private EventsApi() {}
}
