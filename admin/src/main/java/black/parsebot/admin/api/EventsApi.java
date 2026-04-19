package black.parsebot.admin.api;

import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.source.EventsSource;
import black.parsebot.event.Event;

public final class EventsApi {

  public record EventsResponse(String logDir, List<Event> events) {}

  public static final class EventsHandler extends JsonHandler {
    private final EventsSource source;

    public EventsHandler(EventsSource source) {
      this.source = source;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      return source.load();
    }
  }

  private EventsApi() {}
}
