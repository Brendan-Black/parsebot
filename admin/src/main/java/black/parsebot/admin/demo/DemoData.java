package black.parsebot.admin.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import black.parsebot.config.ConfigKey;
import black.parsebot.event.Event;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;

public final class DemoData {

  private DemoData() {}

  public static final String CUSTOMERS_CSV = """
      customer_number,name,address
      1001,Acme Corporation,123 Main St
      1002,Globex Inc,456 Oak Ave
      1003,Initech LLC,789 Pine Rd
      """;

  public static final String PRODUCTS_CSV = """
      product_id,name,sku
      W-001,Widget,WID-1
      G-002,Gadget,GAD-2
      S-003,Sprocket,SPR-3
      """;

  public static final String PRICE_MATRIX_CSV = """
      customer_number,product_id,price
      1001,W-001,4.50
      1001,G-002,7.25
      1002,W-001,5.00
      """;

  public static final String CUSTOMERS_PATH = "(demo) customers.csv";
  public static final String PRODUCTS_PATH = "(demo) products.csv";
  public static final String PRICE_MATRIX_PATH = "(demo) price-matrix.csv";

  public record DemoMessage(
      String subject, String sender, Instant sentAt, String pdfFilename, byte[] pdfBytes) {}

  public static List<DemoMessage> messages() {
    byte[] pdf1 = DemoPdf.fromLines(List.of(
        "PURCHASE ORDER #1234",
        "From: Acme Corporation",
        "",
        "Line items:",
        "  10 x Widget  @ $4.50",
        "   2 x Gadget  @ $7.25"
    ));
    byte[] pdf2 = DemoPdf.fromLines(List.of(
        "REQUEST FOR QUOTE #5678",
        "From: Globex Inc",
        "",
        "Please quote:",
        "   5 x Widget",
        "   1 x Sprocket"
    ));
    return List.of(
        new DemoMessage(
            "Purchase Order #1234",
            "Acme Orders <orders@acme.example.com>",
            Instant.parse("2026-04-18T09:15:00Z"),
            "po-1234.pdf",
            pdf1),
        new DemoMessage(
            "RFQ #5678 for widgets",
            "Globex Sales <sales@globex.example.com>",
            Instant.parse("2026-04-19T14:30:00Z"),
            "rfq-5678.pdf",
            pdf2));
  }

  public static Map<String, String> serviceConfig() {
    Map<String, String> cfg = new LinkedHashMap<>();
    cfg.put(ConfigKey.MAIL_HOST, "imap.demo.local");
    cfg.put(ConfigKey.MAIL_PORT, "993");
    cfg.put(ConfigKey.MAIL_USERNAME, "demo@example.com");
    cfg.put(ConfigKey.MAIL_FOLDER, "INBOX");
    cfg.put(ConfigKey.CLAUDE_API_KEY, "demo-key-not-used");
    cfg.put(ConfigKey.CSV_CUSTOMERS, CUSTOMERS_PATH);
    cfg.put(ConfigKey.CSV_PRODUCTS, PRODUCTS_PATH);
    cfg.put(ConfigKey.CSV_PRICEMATRIX, PRICE_MATRIX_PATH);
    cfg.put(ConfigKey.POLL_INTERVAL_SECONDS, "300");
    return cfg;
  }

  public static List<Event> events() {
    Instant now = Instant.now();
    List<Event> events = new ArrayList<>();

    events.add(event(now.minus(Duration.ofMinutes(12)),
        EventType.MAILBOX_READ_FAILED, EventSeverity.AUDIT,
        "(demo) Failed to connect to imap.demo.local",
        Map.of("host", "imap.demo.local", "error", "connection refused")));
    events.add(event(now.minus(Duration.ofHours(4)),
        EventType.CLAUDE_API_FAILED, EventSeverity.AUDIT,
        "(demo) Claude API returned status 429",
        Map.of("filename", "po-1234.pdf", "statusCode", "429")));

    int[][] reports = {
        {0, 3, 0}, {1, 5, 0}, {2, 4, 1}, {3, 6, 0},
        {4, 2, 0}, {5, 7, 0}, {6, 0, 0}, {7, 4, 0},
        {8, 9, 1}, {9, 3, 0}, {10, 5, 0}, {11, 6, 2},
        {12, 4, 0}, {13, 8, 0},
    };
    for (int[] r : reports) {
      int daysAgo = r[0];
      int success = r[1];
      int failed = r[2];
      int total = success + failed;
      events.add(event(now.minus(Duration.ofDays(daysAgo)).minus(Duration.ofHours(6)),
          EventType.REPORT_CARD, EventSeverity.INFO,
          String.format("(demo) Report card: %d processed, %d succeeded, %d failed", total, success, failed),
          Map.of(
              "totalProcessed", String.valueOf(total),
              "successCount", String.valueOf(success),
              "failCount", String.valueOf(failed))));
    }
    return events;
  }

  private static Event event(Instant timestamp, EventType type, EventSeverity severity,
      String message, Map<String, String> details) {
    return new Event(UUID.randomUUID().toString(), type, severity, timestamp, message, details);
  }
}
