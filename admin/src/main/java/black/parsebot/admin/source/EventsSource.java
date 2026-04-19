package black.parsebot.admin.source;

import black.parsebot.admin.api.EventsApi.EventsResponse;

public interface EventsSource {

  EventsResponse load();
}
