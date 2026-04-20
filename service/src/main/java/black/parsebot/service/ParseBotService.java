package black.parsebot.service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.config.AppConfig;
import black.parsebot.config.CustomOverrideResolver;
import black.parsebot.config.MailConfig;
import black.parsebot.config.ReferenceDataConfig;
import black.parsebot.event.Event;
import black.parsebot.event.EventBus;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import black.parsebot.model.TransformedData;
import black.parsebot.model.raw.RawMailboxData;
import black.parsebot.parser.ClaudeClient;
import black.parsebot.parser.DefaultPdfValidator;
import black.parsebot.parser.PdfValidator;
import black.parsebot.persistence.ProcessingHistoryRepository;
import black.parsebot.processor.MailboxProcessor;
import black.parsebot.parser.PriceMatrix;
import black.parsebot.reader.MailboxReader;
import black.parsebot.writer.MailboxWriter;
import black.parsebot.writer.SftpWriter;

public class ParseBotService {

  private static final Logger log = LoggerFactory.getLogger(ParseBotService.class);

  private final MailboxReader mailReader;
  private final MailboxWriter mailWriter;
  private final HttpClient httpClient;
  private final ClaudeClient claudeClient;
  private final SftpWriter sftpWriter;
  private final MailConfig mailConfig;
  private final Path customerCsvPath;
  private final Path productCsvPath;
  private final CustomOverrideResolver overrideResolver;
  private final AppConfig config;
  private final EventBus eventBus;
  private final ProcessingHistoryRepository processingHistory;
  private final int historyPerEmail;
  private final int consecutiveFailureThreshold;

  // Cumulative counters for report card — reset by ReportCardScheduler
  private final AtomicInteger totalSuccessCount = new AtomicInteger();
  private final AtomicInteger totalFailCount = new AtomicInteger();

  public ParseBotService(AppConfig config, EventBus eventBus,
                         ProcessingHistoryRepository processingHistory) throws IOException {
    this.config = config;
    this.eventBus = eventBus;
    this.processingHistory = processingHistory;
    this.historyPerEmail = config.getPersistenceConfig().getHistoryPerEmail();
    this.consecutiveFailureThreshold = config.getEventsConfig().getConsecutiveFailureThreshold();
    this.mailConfig = config.getMailConfig();
    this.mailReader = new MailboxReader(config.getMailConfig(), eventBus);
    this.mailWriter = new MailboxWriter(mailReader, eventBus);

    PdfValidator pdfValidator = new DefaultPdfValidator(
        config.getClaudeConfig().getPdfMaxBytes(),
        config.getClaudeConfig().getPdfMaxPages());
    this.httpClient = HttpClient.newHttpClient();
    this.claudeClient = new ClaudeClient(httpClient, ClaudeClient.DEFAULT_API_URL,
        config.getClaudeConfig().getApiKey(), eventBus, pdfValidator);
    ReferenceDataConfig refData = config.getReferenceDataConfig();
    this.customerCsvPath = Path.of(refData.getCustomerCsvPath());
    this.productCsvPath = Path.of(refData.getProductCsvPath());
    this.overrideResolver = new CustomOverrideResolver(refData.getCustomRulesDir(), refData.getCustomProductListsDir());

    this.sftpWriter = new SftpWriter(config.getSftpConfig(), eventBus);
  }

  public AtomicInteger getTotalSuccessCount() {
    return totalSuccessCount;
  }

  public AtomicInteger getTotalFailCount() {
    return totalFailCount;
  }

  public void run() {
    try {
      log.info("Starting pipeline run");

      String customerCsv = Files.readString(customerCsvPath);
      String productCsv = Files.readString(productCsvPath);

      PriceMatrix priceMatrix = null;
      String priceMatrixPath = config.getReferenceDataConfig().getPriceMatrixCsvPath();
      if (!priceMatrixPath.isBlank()) {
        Path matrixPath = Path.of(priceMatrixPath);
        if (Files.exists(matrixPath)) {
          priceMatrix = PriceMatrix.load(Files.readString(matrixPath));
        } else {
          log.warn("Price matrix CSV configured but not found: {}", matrixPath);
        }
      }

      MailboxProcessor processor = new MailboxProcessor(
          claudeClient, customerCsv, productCsv, priceMatrix,
          processingHistory, historyPerEmail, consecutiveFailureThreshold, eventBus);

      // Collect raw data from all sources
      List<RawMailboxData> rawData = new ArrayList<>();
      rawData.addAll(mailReader.read());

      if (rawData.isEmpty()) {
        log.info("No data to process");
        return;
      }

      // Process each item individually
      int successCount = 0;
      int failCount = 0;

      for (RawMailboxData email : rawData) {
        try {
          String sender = email.getSenderEmail();
          String customRules = overrideResolver.resolveRules(sender);
          String customProductList = overrideResolver.resolveProductList(sender);

          if (customRules != null || customProductList != null) {
            log.info("Using custom overrides for sender '{}'", sender);
          }

          List<TransformedData> transformed = processor.process(email, customRules, customProductList);
          if (!transformed.isEmpty()) {
            sftpWriter.write(transformed);
          }
          onSuccess(email, mailWriter);
          successCount++;
          totalSuccessCount.incrementAndGet();
        } catch (Exception e) {
          log.error("Failed to process: {}", email.getName(), e);
          eventBus.publish(Event.create(
              EventType.MESSAGE_PROCESSING_FAILED,
              EventSeverity.AUDIT,
              "Failed to process message: " + email.getName(),
              Map.of(
                  "messageName", email.getName() != null ? email.getName() : "<unknown>",
                  "sender", email.getSenderEmail() != null ? email.getSenderEmail() : "<unknown>",
                  "error", String.valueOf(e.getMessage())
              )));
          onFailure(email, mailWriter);
          failCount++;
          totalFailCount.incrementAndGet();
        }
      }

      log.info("Pipeline run complete: {} succeeded, {} failed", successCount, failCount);
    } catch (Exception e) {
      log.error("Pipeline run failed", e);
      eventBus.publish(Event.create(
          EventType.PIPELINE_RUN_FAILED,
          EventSeverity.AUDIT,
          "Pipeline run failed: " + e.getMessage(),
          Map.of("error", String.valueOf(e.getMessage()))));
    }
  }

  public void close() {
    mailReader.close();
    httpClient.close();
  }

  private void onSuccess(RawMailboxData mbd, MailboxWriter mailWriter) {
    mailWriter.moveToFolder(mbd.getSourceMessage(), mailConfig.getSuccessFolder());
  }

  private void onFailure(RawMailboxData mbd, MailboxWriter mailWriter) {
    mailWriter.moveToFolder(mbd.getSourceMessage(), mailConfig.getFailureFolder());
  }
}
