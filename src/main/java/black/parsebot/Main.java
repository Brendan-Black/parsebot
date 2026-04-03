package black.parsebot;

import black.parsebot.config.AppConfig;
import black.parsebot.service.ParseBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("ParseBot starting up");

        AppConfig config = AppConfig.load();
        ParseBotService service = new ParseBotService(config);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        long intervalSeconds = config.getPollIntervalSeconds();

        scheduler.scheduleWithFixedDelay(service::run, 0, intervalSeconds, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("ParseBot shutting down");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }
}
