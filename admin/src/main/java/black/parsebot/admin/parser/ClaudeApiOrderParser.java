package black.parsebot.admin.parser;

import java.net.http.HttpClient;

import black.parsebot.event.EventPublisher;
import black.parsebot.parser.ClaudeClient;
import black.parsebot.parser.DefaultPdfValidator;
import black.parsebot.parser.PdfValidator;

public final class ClaudeApiOrderParser implements OrderParser {

  private static final EventPublisher NOOP = event -> {};

  private final HttpClient httpClient;
  private final PdfValidator pdfValidator;

  public ClaudeApiOrderParser(HttpClient httpClient) {
    this(httpClient, new DefaultPdfValidator(
        DefaultPdfValidator.DEFAULT_MAX_BYTES, DefaultPdfValidator.DEFAULT_MAX_PAGES));
  }

  public ClaudeApiOrderParser(HttpClient httpClient, PdfValidator pdfValidator) {
    this.httpClient = httpClient;
    this.pdfValidator = pdfValidator;
  }

  @Override
  public String parsePdf(
      String filename,
      byte[] pdfBytes,
      String customerCsv,
      String productCsv,
      String apiKey,
      String customRules
  ) throws Exception {
    ClaudeClient client = new ClaudeClient(httpClient, ClaudeClient.DEFAULT_API_URL,
        apiKey, NOOP, pdfValidator);
    return client.parsePdf(filename, pdfBytes, customerCsv, productCsv, customRules);
  }
}
