package black.parsebot.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPdfValidatorTest {

  private static byte[] pdfWithPages(int pageCount) {
    StringBuilder sb = new StringBuilder("%PDF-1.4\n");
    for (int i = 0; i < pageCount; i++) {
      sb.append(i + 1).append(" 0 obj\n<< /Type /Page /Parent 1 0 R >>\nendobj\n");
    }
    sb.append("trailer << /Root 1 0 R >>\n%%EOF\n");
    return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
  }

  @Test
  void acceptsSmallValidPdf() {
    DefaultPdfValidator v = new DefaultPdfValidator(1024 * 1024, 20);
    assertDoesNotThrow(() -> v.validate("ok.pdf", pdfWithPages(5)));
  }

  @Test
  void rejectsEmptyBytes() {
    DefaultPdfValidator v = new DefaultPdfValidator(1024, 5);
    PdfValidationException ex = assertThrows(PdfValidationException.class,
        () -> v.validate("empty.pdf", new byte[0]));
    assertTrue(ex.getMessage().toLowerCase().contains("empty"));
  }

  @Test
  void rejectsNullBytes() {
    DefaultPdfValidator v = new DefaultPdfValidator(1024, 5);
    assertThrows(PdfValidationException.class, () -> v.validate("null.pdf", null));
  }

  @Test
  void rejectsNonPdfMagic() {
    DefaultPdfValidator v = new DefaultPdfValidator(1024, 5);
    byte[] notPdf = "PK\u0003\u0004some-zip-contents".getBytes(StandardCharsets.ISO_8859_1);
    PdfValidationException ex = assertThrows(PdfValidationException.class,
        () -> v.validate("fake.pdf", notPdf));
    assertTrue(ex.getMessage().contains("not a PDF"));
  }

  @Test
  void rejectsOversizedPdf() {
    DefaultPdfValidator v = new DefaultPdfValidator(16, 5);
    PdfValidationException ex = assertThrows(PdfValidationException.class,
        () -> v.validate("big.pdf", pdfWithPages(1)));
    assertTrue(ex.getMessage().contains("size limit"));
  }

  @Test
  void rejectsPdfWithTooManyPages() {
    DefaultPdfValidator v = new DefaultPdfValidator(10 * 1024 * 1024, 20);
    PdfValidationException ex = assertThrows(PdfValidationException.class,
        () -> v.validate("long.pdf", pdfWithPages(25)));
    assertTrue(ex.getMessage().contains("page limit"));
  }

  @Test
  void acceptsPdfAtPageLimit() {
    DefaultPdfValidator v = new DefaultPdfValidator(10 * 1024 * 1024, 20);
    assertDoesNotThrow(() -> v.validate("boundary.pdf", pdfWithPages(20)));
  }

  @Test
  void pageRegexDoesNotMatchPagesTreeNode() {
    DefaultPdfValidator v = new DefaultPdfValidator(10 * 1024 * 1024, 1);
    // /Type /Pages is the tree root, not a page — must not count toward the limit.
    String body = "%PDF-1.4\n1 0 obj\n<< /Type /Pages /Count 1 /Kids [2 0 R] >>\nendobj\n"
        + "2 0 obj\n<< /Type /Page >>\nendobj\n%%EOF\n";
    assertDoesNotThrow(() -> v.validate("tree.pdf", body.getBytes(StandardCharsets.ISO_8859_1)));
  }

  @Test
  void skipsPageCheckWhenMarkersNotVisible() {
    // Object-stream-compressed PDFs have no literal /Type /Page markers in the outer stream —
    // we can't count, so we don't reject on page count. Size is the backstop.
    DefaultPdfValidator v = new DefaultPdfValidator(10 * 1024 * 1024, 1);
    byte[] minimal = "%PDF-1.7\n...opaque-object-stream-body...\n%%EOF\n"
        .getBytes(StandardCharsets.ISO_8859_1);
    assertDoesNotThrow(() -> v.validate("opaque.pdf", minimal));
  }

  @Test
  void rejectsInvalidConstructorArgs() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultPdfValidator(0, 10));
    assertThrows(IllegalArgumentException.class, () -> new DefaultPdfValidator(100, 0));
  }
}
