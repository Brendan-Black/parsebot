package black.parsebot.admin.demo;

import black.parsebot.admin.api.EventsApi.EventsResponse;
import black.parsebot.admin.source.EventsSource;

public final class DemoEventsSource implements EventsSource {

  @Override
  public EventsResponse load() {
    return new EventsResponse("(demo) in-memory", DemoData.events());
  }
}
