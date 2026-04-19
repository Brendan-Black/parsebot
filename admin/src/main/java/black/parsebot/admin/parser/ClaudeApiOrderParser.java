package black.parsebot.admin.parser;

import black.parsebot.event.EventPublisher;
import black.parsebot.parser.ClaudeClient;

public final class ClaudeApiOrderParser implements OrderParser {

  private static final EventPublisher NOOP = event -> {};

  @Override
  public String parsePdf(
      String filename,
      byte[] pdfBytes,
      String customerCsv,
      String productCsv,
      String apiKey,
      String customRules
  ) throws Exception {
    ClaudeClient client = new ClaudeClient(apiKey, NOOP);
    return client.parsePdf(filename, pdfBytes, customerCsv, productCsv, customRules);
  }
}
