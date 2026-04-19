package black.parsebot.admin.parser;

public interface OrderParser {

  String parsePdf(
      String filename,
      byte[] pdfBytes,
      String customerCsv,
      String productCsv,
      String apiKey,
      String customRules
  ) throws Exception;
}
