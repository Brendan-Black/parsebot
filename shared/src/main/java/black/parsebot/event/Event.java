package black.parsebot.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Event(
		String id,
		EventType type,
		EventSeverity severity,
		Instant timestamp,
		String message,
		Map<String, String> details
) {

	public static Event create(EventType type, EventSeverity severity, String message, Map<String, String> details) {
		return new Event(UUID.randomUUID().toString(), type, severity, Instant.now(), message, details);
	}

}
