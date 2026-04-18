package black.parsebot.checkevents;

import java.time.Instant;
import java.util.Map;

record Event(
        String id,
        String type,
        String severity,
        Instant timestamp,
        String message,
        Map<String, String> details
) {}
