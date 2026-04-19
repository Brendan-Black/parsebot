package black.parsebot.admin.api;

import java.util.Base64;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.parser.OrderParser;
import black.parsebot.admin.server.JsonHandler;
import black.parsebot.parser.PriceMatrix;

public final class TestParseApi {

  public record TestParseRequest(
      String filename,
      String pdfBase64,
      String customerCsv,
      String productCsv,
      String apiKey,
      String customRules,
      String priceMatrixCsv
  ) {}

  public record TestParseResponse(String response, long durationMs) {}

  public static final class Handler extends JsonHandler {
    private final OrderParser parser;
    private final boolean requireApiKey;

    public Handler(OrderParser parser, boolean requireApiKey) {
      this.parser = parser;
      this.requireApiKey = requireApiKey;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) throws Exception {
      requireMethod(exchange, "POST");
      TestParseRequest req = readJson(exchange, TestParseRequest.class);
      if (req == null) throw new HttpException(400, "Missing request body");
      if (requireApiKey && (req.apiKey == null || req.apiKey.isBlank())) {
        throw new HttpException(400, "apiKey is required");
      }
      if (req.pdfBase64 == null || req.pdfBase64.isBlank()) throw new HttpException(400, "pdfBase64 is required");
      if (req.customerCsv == null) throw new HttpException(400, "customerCsv is required");
      if (req.productCsv == null) throw new HttpException(400, "productCsv is required");

      byte[] pdfBytes;
      try {
        pdfBytes = Base64.getDecoder().decode(req.pdfBase64);
      } catch (IllegalArgumentException e) {
        throw new HttpException(400, "Invalid base64 PDF content");
      }

      String filename = (req.filename != null && !req.filename.isBlank()) ? req.filename : "test.pdf";
      String customRules = (req.customRules != null && !req.customRules.isBlank()) ? req.customRules : null;

      long start = System.currentTimeMillis();
      String response = parser.parsePdf(filename, pdfBytes, req.customerCsv, req.productCsv, req.apiKey, customRules);

      if (req.priceMatrixCsv != null && !req.priceMatrixCsv.isBlank()) {
        response = PriceMatrix.load(req.priceMatrixCsv).applyToResponse(response);
      }

      return new TestParseResponse(response, System.currentTimeMillis() - start);
    }
  }

  private TestParseApi() {}
}
