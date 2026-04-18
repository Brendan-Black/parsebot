package black.parsebot.event;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsecutiveFailureDetector {

  private static final Logger log = LoggerFactory.getLogger(ConsecutiveFailureDetector.class);

  private final int threshold;
  private final EventBus eventBus;
  private int consecutiveFailures = 0;

  public ConsecutiveFailureDetector(int threshold, EventBus eventBus) {
    this.threshold = threshold;
    this.eventBus = eventBus;
  }

  public void recordSuccess() {
    consecutiveFailures = 0;
  }

  public void recordFailure(String emailName, String errorMessage) {
    consecutiveFailures++;
    if (consecutiveFailures == threshold) {
      log.warn("{} consecutive failures reached — firing event", threshold);
      eventBus.publish(Event.create(
          EventType.CONSECUTIVE_FAILURES,
          EventSeverity.CRITICAL,
          threshold + " consecutive email processing failures",
          Map.of(
              "lastFailedEmail", emailName != null ? emailName : "unknown",
              "errorSummary", errorMessage != null ? errorMessage : "unknown"
          )
      ));
      // Reset so we fire again after another N consecutive failures
      consecutiveFailures = 0;
    }
  }
}
