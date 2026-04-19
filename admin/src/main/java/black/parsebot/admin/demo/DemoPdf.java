package black.parsebot.admin.demo;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class DemoPdf {

  private DemoPdf() {}

  static byte[] fromLines(List<String> lines) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    List<Integer> offsets = new ArrayList<>();

    write(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

    offsets.add(out.size());
    write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

    offsets.add(out.size());
    write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

    offsets.add(out.size());
    write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
        + "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

    StringBuilder content = new StringBuilder("BT\n/F1 12 Tf\n72 740 Td\n14 TL\n");
    for (int i = 0; i < lines.size(); i++) {
      if (i > 0) content.append("T*\n");
      content.append('(').append(escape(lines.get(i))).append(") Tj\n");
    }
    content.append("ET\n");
    byte[] streamBytes = content.toString().getBytes(StandardCharsets.US_ASCII);

    offsets.add(out.size());
    write(out, "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
    out.writeBytes(streamBytes);
    write(out, "endstream\nendobj\n");

    offsets.add(out.size());
    write(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

    int xrefOffset = out.size();
    write(out, "xref\n0 6\n0000000000 65535 f \n");
    for (int off : offsets) {
      write(out, String.format("%010d 00000 n \n", off));
    }

    write(out, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF");
    return out.toByteArray();
  }

  private static void write(ByteArrayOutputStream out, String s) {
    out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
  }

  private static String escape(String s) {
    return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
  }
}
