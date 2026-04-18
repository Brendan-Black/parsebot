package black.parsebot.checkevents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import black.parsebot.event.Event;
import black.parsebot.storage.Slf4jStorage;
import black.parsebot.storage.Storage;

public class Main {

    public static void main(String[] args) {
        Path logDir = resolveLogDir(args);

        if (!Files.isDirectory(logDir)) {
            System.err.println("Log directory not found: " + logDir);
            System.err.println("Usage: check-events [--logs <directory>]");
            System.exit(1);
        }

        Storage<Event> storage = new Slf4jStorage<>(Event.class, "EVENT: ", logDir, "parsebot*.log");

        List<Event> events;
        try {
            events = storage.readAll();
        } catch (IOException e) {
            System.err.println("Failed to read log files: " + e.getMessage());
            System.exit(1);
            return;
        }

        EventsUi ui = new PowerShellEventsUi();
        if (events.isEmpty()) {
            ui.showEmpty(logDir);
        } else {
            ui.showEvents(events, logDir);
        }
    }

    private static Path resolveLogDir(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--logs".equals(args[i])) {
                return Path.of(args[i + 1]).toAbsolutePath();
            }
        }
        return Path.of("logs").toAbsolutePath();
    }
}
