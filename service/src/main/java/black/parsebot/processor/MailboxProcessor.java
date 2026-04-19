package black.parsebot.processor;

import black.parsebot.event.Event;
import black.parsebot.event.EventPublisher;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import black.parsebot.model.TransformedData;
import black.parsebot.model.raw.RawMailboxData;
import black.parsebot.parser.ClaudeClient;
import black.parsebot.parser.PdfValidationException;
import black.parsebot.parser.PriceMatrix;
import black.parsebot.persistence.ProcessingHistoryRepository;
import black.parsebot.persistence.ProcessingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MailboxProcessor {

  private static final Logger log = LoggerFactory.getLogger(MailboxProcessor.class);

  private final ClaudeClient claudeClient;
  private final String customerCsv;
  private final String productCsv;
  private final PriceMatrix priceMatrix;
  private final ProcessingHistoryRepository history;
  private final int historyLimit;
  private final int consecutiveFailureThreshold;
  private final EventPublisher eventPublisher;

  public MailboxProcessor(ClaudeClient claudeClient,
                          String customerCsv,
                          String productCsv,
                          PriceMatrix priceMatrix,
                          ProcessingHistoryRepository history,
                          int historyLimit,
                          int consecutiveFailureThreshold,
                          EventPublisher eventPublisher) {
    this.claudeClient = claudeClient;
    this.customerCsv = customerCsv;
    this.productCsv = productCsv;
    this.priceMatrix = priceMatrix;
    this.history = history;
    this.historyLimit = historyLimit;
    this.consecutiveFailureThreshold = consecutiveFailureThreshold;
    this.eventPublisher = eventPublisher;
  }

  public List<TransformedData> process(RawMailboxData raw) throws IOException, InterruptedException, PdfValidationException {
    return process(raw, null, null);
  }

  public List<TransformedData> process(RawMailboxData raw, String customRules, String customProductCsv) throws IOException, InterruptedException, PdfValidationException {
    log.info("Processing: {} (source: {})", raw.getName(), raw.getClass().getSimpleName());

    List<Map.Entry<String, byte[]>> pdfs;
    try {
      pdfs = raw.extractPdfAttachments();
    } catch (Exception e) {
      throw new IOException("Failed to extract PDF attachments from: " + raw.getName(), e);
    }

    if (pdfs.isEmpty()) {
      log.warn("No PDF attachments found in: {}", raw.getName());
      return List.of();
    }

    String sender = raw.getSenderEmail();
    Set<String> seenHashes = new HashSet<>();
    for (ProcessingRecord r : history.findRecentByEmailAddress(sender, historyLimit)) {
      seenHashes.add(r.getPdfHash());
    }

    String effectiveProductCsv = (customProductCsv != null && !customProductCsv.isBlank()) ?
        customProductCsv
        : productCsv;

    List<TransformedData> results = new ArrayList<>();

    for (Map.Entry<String, byte[]> pdf : pdfs) {
      String pdfName = pdf.getKey();
      byte[] pdfBytes = pdf.getValue();
      String hash = sha256Hex(pdfBytes);

      if (seenHashes.contains(hash)) {
        log.info("Skipping duplicate PDF '{}' from '{}' (hash {})", pdfName, sender, hash);
        continue;
      }

      try {
        log.info("Sending PDF attachment '{}' to Claude", pdfName);
        String responseJson = claudeClient.parsePdf(pdfName, pdfBytes, customerCsv, effectiveProductCsv, customRules);

        if (priceMatrix != null) {
          responseJson = priceMatrix.applyToResponse(responseJson);
        }

        String outputFilename = sanitizeFilename(pdfName) + ".json";
        results.add(new TransformedData(outputFilename, responseJson.getBytes(StandardCharsets.UTF_8)));
        history.save(new ProcessingRecord(sender, hash, true, Instant.now()));
      } catch (IOException | InterruptedException | PdfValidationException | RuntimeException e) {
        history.save(new ProcessingRecord(sender, hash, false, Instant.now()));
        checkConsecutiveFailures(sender, pdfName, e);
        throw e;
      }
    }

    return results;
  }

  private void checkConsecutiveFailures(String sender, String pdfName, Throwable cause) {
    int threshold = consecutiveFailureThreshold;
    List<ProcessingRecord> lookback = history.findRecentByEmailAddress(sender, threshold + 1);
    if (lookback.size() < threshold) return;
    for (int i = 0; i < threshold; i++) {
      if (lookback.get(i).isSuccess()) return;
    }
    boolean priorWasSuccessOrAbsent = lookback.size() == threshold || lookback.get(threshold).isSuccess();
    if (!priorWasSuccessOrAbsent) return;

    log.warn("{} consecutive failures for sender '{}' — firing event", threshold, sender);
    eventPublisher.publish(Event.create(
        EventType.CONSECUTIVE_FAILURES,
        EventSeverity.CRITICAL,
        threshold + " consecutive processing failures from " + displaySender(sender),
        Map.of(
            "sender", displaySender(sender),
            "lastPdf", pdfName != null ? pdfName : "unknown",
            "errorSummary", cause != null && cause.getMessage() != null ? cause.getMessage() : "unknown"
        )));
  }

  private static String displaySender(String sender) {
    return sender == null || sender.isBlank() ? "unknown" : sender;
  }

  private static String sha256Hex(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private String sanitizeFilename(String name) {
    if (name == null || name.isBlank()) {
      return "unnamed";
    }
    String withoutExtension = name.replaceFirst("\\.[^.]+$", "");
    return withoutExtension.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
