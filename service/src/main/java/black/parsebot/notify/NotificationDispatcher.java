package black.parsebot.notify;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.event.Event;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import black.parsebot.storage.Storage;

public class NotificationDispatcher {

  private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

  private final List<NotificationChannel> criticalChannels;
  private final List<NotificationChannel> infoChannels;
  private final Storage<Event> storage;

  public NotificationDispatcher(List<NotificationChannel> criticalChannels,
                 List<NotificationChannel> infoChannels,
                 Storage<Event> storage) {
    this.criticalChannels = criticalChannels;
    this.infoChannels = infoChannels;
    this.storage = storage;
  }

  public void dispatch(Event event) {
    List<NotificationChannel> targets = switch (event.severity()) {
      case CRITICAL -> criticalChannels;
      case INFO -> infoChannels;
      case AUDIT -> List.of();
    };

    for (NotificationChannel channel : targets) {
      try {
        channel.send(event);
      } catch (Exception e) {
        String channelName = channel.getClass().getSimpleName();
        log.error("Notification channel {} failed for event {}", channelName, event.id(), e);
        // Persist directly — going through the bus would loop back into dispatch()
        storage.append(Event.create(
            EventType.NOTIFICATION_FAILED,
            EventSeverity.AUDIT,
            "Notification channel " + channelName + " failed for event " + event.id(),
            Map.of(
                "channel", channelName,
                "originalEventId", event.id(),
                "originalEventType", String.valueOf(event.type()),
                "error", String.valueOf(e.getMessage())
            )));
      }
    }
  }
}
