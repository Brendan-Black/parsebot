package black.parsebot.admin.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClaudeCliOrderParser implements OrderParser {

  private static final Logger log = LoggerFactory.getLogger(ClaudeCliOrderParser.class);
  private static final long TIMEOUT_SECONDS = 120;

  private final String command;

  public ClaudeCliOrderParser() {
    this("claude");
  }

  public ClaudeCliOrderParser(String command) {
    this.command = command;
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
    Path tempPdf = Files.createTempFile("parsebot-demo-", "-" + sanitize(filename));
    try {
      Files.write(tempPdf, pdfBytes);
      String prompt = buildPrompt(tempPdf, customerCsv, productCsv, customRules);
      return runClaude(prompt);
    } finally {
      try {
        Files.deleteIfExists(tempPdf);
      } catch (IOException e) {
        log.warn("Failed to delete temp PDF {}: {}", tempPdf, e.getMessage());
      }
    }
  }

  private String runClaude(String prompt) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command, "-p", prompt)
        .redirectErrorStream(true);
    log.info("Invoking local claude CLI ({})", command);
    Process proc = pb.start();

    StringBuilder out = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        out.append(line).append('\n');
      }
    }

    if (!proc.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      proc.destroyForcibly();
      throw new IOException("claude CLI timed out after " + TIMEOUT_SECONDS + "s");
    }
    int rc = proc.exitValue();
    if (rc != 0) {
      throw new IOException("claude CLI exit " + rc + ": " + out);
    }
    return out.toString().trim();
  }

  private static String buildPrompt(Path pdfPath, String customerCsv, String productCsv, String customRules) {
    String instructions = (customRules != null && !customRules.isBlank())
        ? customRules
        : "Read the PDF and identify the customer and line-item products. "
            + "Best-effort match against the customer list and product catalog below. "
            + "Respond with a concise summary of the matched customer and items.";

    return """
        %s

        PDF to parse: %s

        --- CUSTOMER LIST (CSV) ---
        %s

        --- PRODUCT CATALOG (CSV) ---
        %s
        """.formatted(instructions, pdfPath.toAbsolutePath(), customerCsv, productCsv);
  }

  private static String sanitize(String filename) {
    if (filename == null || filename.isBlank()) return "demo.pdf";
    return filename.replaceAll("[^A-Za-z0-9._-]", "_");
  }
}
