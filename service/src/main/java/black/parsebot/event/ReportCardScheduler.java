package black.parsebot.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.config.AppConfig;

public class ReportCardScheduler implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(ReportCardScheduler.class);

	private final LocalTime reportTime;
	private final EventBus eventBus;
	private final AtomicInteger successCount;
	private final AtomicInteger failCount;
	private LocalDate lastReportDate;

	public ReportCardScheduler(AppConfig.EventsConfig config, EventBus eventBus,  AtomicInteger successCount, AtomicInteger failCount) {
		this.reportTime = LocalTime.parse(config.getReportTime(), DateTimeFormatter.ofPattern("HH:mm"));
		this.eventBus = eventBus;
		this.successCount = successCount;
		this.failCount = failCount;
	}

	@Override
	public void run() {
		try {
			LocalDate today = LocalDate.now();
			LocalTime now = LocalTime.now();

			if (today.equals(lastReportDate)) {
				return;
			}

			if (now.isBefore(reportTime)) {
				return;
			}

			int success = successCount.getAndSet(0);
			int fail = failCount.getAndSet(0);
			int total = success + fail;

			String message = String.format("Report card: %d processed, %d succeeded, %d failed",
					total, success, fail);

			eventBus.publish(Event.create(
					EventType.REPORT_CARD,
					EventSeverity.INFO,
					message,
					Map.of(
							"totalProcessed", String.valueOf(total),
							"successCount", String.valueOf(success),
							"failCount", String.valueOf(fail)
					)
			));

			lastReportDate = today;
			log.info("Report card generated for {}", today);
		} catch (Exception e) {
			log.error("Failed to generate report card", e);
		}
	}
}
