package black.parsebot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.config.AppConfig;
import black.parsebot.event.EventBus;
import black.parsebot.event.ReportCardScheduler;
import black.parsebot.notify.NotificationChannel;
import black.parsebot.notify.NotificationDispatcher;
import black.parsebot.notify.SmtpChannel;
import black.parsebot.notify.TeamsWebhookChannel;
import black.parsebot.service.ParseBotService;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("ParseBot starting up");

        AppConfig config = AppConfig.load();

        // Initialize event system
        AppConfig.EventsConfig eventsConfig = config.getEventsConfig();
        AppConfig.NotifyConfig notifyConfig = config.getNotifyConfig();

        // Build notification channels
        List<NotificationChannel> criticalChannels = new ArrayList<>();
        List<NotificationChannel> infoChannels = new ArrayList<>();

        if (notifyConfig.isSmtpEnabled()) {
            String to = notifyConfig.getSmtpTo();
            String toUrgent = notifyConfig.getSmtpToUrgent();
            if (!to.isBlank()) {
                infoChannels.add(new SmtpChannel(notifyConfig, config.getMailConfig(), to));
                log.info("SMTP notification channel enabled for info events");
            }
            if (!toUrgent.isBlank()) {
                criticalChannels.add(new SmtpChannel(notifyConfig, config.getMailConfig(), toUrgent));
                log.info("SMTP notification channel enabled for urgent events");
            }
        }
        if (notifyConfig.isTeamsEnabled()) {
            criticalChannels.add(new TeamsWebhookChannel(notifyConfig));
            log.info("Teams webhook notification channel enabled");
        }

        NotificationDispatcher dispatcher = new NotificationDispatcher(criticalChannels, infoChannels);
        EventBus eventBus = new EventBus(dispatcher);

        ParseBotService service;
        try {
            service = new ParseBotService(config, eventBus);
        } catch (IOException e) {
            log.error("Failed to initialize ParseBotService", e);
            return;
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        long intervalSeconds = config.getGeneralConfig().getPollIntervalSeconds();

        // Schedule main pipeline
        scheduler.scheduleWithFixedDelay(service::run, 0, intervalSeconds, TimeUnit.SECONDS);

        // Schedule report card checker (runs every minute)
        ReportCardScheduler reportCard = new ReportCardScheduler(
                eventsConfig, eventBus,
                service.getTotalSuccessCount(), service.getTotalFailCount());
        scheduler.scheduleWithFixedDelay(reportCard, 1, 1, TimeUnit.MINUTES);

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
            service.close();
        }));
    }
}
