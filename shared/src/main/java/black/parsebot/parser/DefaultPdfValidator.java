package black.parsebot.parser;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefaultPdfValidator implements PdfValidator {

  // Anthropic's hard cap is 32 MB; default well below that to catch accidental huge uploads.
  public static final long DEFAULT_MAX_BYTES = 10L * 1024 * 1024;
  public static final int DEFAULT_MAX_PAGES = 20;

  private static final byte[] PDF_MAGIC = { '%', 'P', 'D', 'F', '-' };

  // Matches /Type /Page where /Page is NOT followed by another letter (so /Pages is excluded).
  // PDFs are byte-oriented but structural markers are ASCII, so ISO-8859-1 decoding is safe.
  private static final Pattern PAGE_OBJECT_PATTERN =
      Pattern.compile("/Type\\s*/Page(?![A-Za-z])");

  private final long maxBytes;
  private final int maxPages;

  public DefaultPdfValidator(long maxBytes, int maxPages) {
    if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
    if (maxPages <= 0) throw new IllegalArgumentException("maxPages must be positive");
    this.maxBytes = maxBytes;
    this.maxPages = maxPages;
  }

  @Override
  public void validate(String filename, byte[] pdfBytes) throws PdfValidationException {
    if (pdfBytes == null || pdfBytes.length == 0) {
      throw new PdfValidationException("PDF is empty");
    }
    if (!hasPdfMagic(pdfBytes)) {
      throw new PdfValidationException("file is not a PDF (missing %PDF- header)");
    }
    if (pdfBytes.length > maxBytes) {
      throw new PdfValidationException(
          "PDF exceeds size limit: " + pdfBytes.length + " bytes > " + maxBytes + " bytes");
    }
    int pages = countPages(pdfBytes);
    // countPages returns -1 when page markers aren't visible (e.g. object-stream compressed).
    // In that case we can't prove the page count, so we let the size check be the backstop.
    if (pages > maxPages) {
      throw new PdfValidationException(
          "PDF exceeds page limit: " + pages + " pages > " + maxPages + " pages");
    }
  }

  private static boolean hasPdfMagic(byte[] bytes) {
    if (bytes.length < PDF_MAGIC.length) return false;
    for (int i = 0; i < PDF_MAGIC.length; i++) {
      if (bytes[i] != PDF_MAGIC[i]) return false;
    }
    return true;
  }

  private static int countPages(byte[] bytes) {
    String text = new String(bytes, StandardCharsets.ISO_8859_1);
    Matcher m = PAGE_OBJECT_PATTERN.matcher(text);
    int count = 0;
    while (m.find()) count++;
    return count == 0 ? -1 : count;
  }
}
