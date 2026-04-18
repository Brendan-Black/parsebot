package black.parsebot.checkevents;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

final class EventLogParser {

    private static final String EVENT_MARKER = "EVENT: ";

    private EventLogParser() {}

    static List<Event> parseEventsFromLogs(Path logDir) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, type, ctx) ->
                        Instant.parse(json.getAsString()))
                .create();

        // Collect all parsebot log files (current + rotated)
        List<Path> logFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, "parsebot*.log")) {
            for (Path path : stream) {
                logFiles.add(path);
            }
        }
        // Sort by filename so rotated logs appear in chronological order
        logFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));

        List<Event> events = new ArrayList<>();
        for (Path logFile : logFiles) {
            for (String line : Files.readAllLines(logFile)) {
                int idx = line.indexOf(EVENT_MARKER);
                if (idx < 0) continue;

                String json = line.substring(idx + EVENT_MARKER.length());
                try {
                    Event event = gson.fromJson(json, Event.class);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    // Skip malformed event lines
                }
            }
        }
        return events;
    }
}
