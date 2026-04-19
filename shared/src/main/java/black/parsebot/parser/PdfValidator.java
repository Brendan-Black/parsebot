package black.parsebot.parser;

public interface PdfValidator {

  void validate(String filename, byte[] pdfBytes) throws PdfValidationException;
}
