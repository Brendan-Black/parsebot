package black.parsebot.ps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PowerShellRunner {

  public static final String WINFORMS_PREAMBLE = """
      Add-Type -AssemblyName System.Windows.Forms
      Add-Type -AssemblyName System.Drawing
      [System.Windows.Forms.Application]::EnableVisualStyles()
      """;

  private PowerShellRunner() {}

  public static String escapeSingleQuote(String value) {
    return value.replace("'", "''");
  }

  public static String escapeSingleLine(String value) {
    return escapeSingleQuote(value).replace("\n", " ").replace("\r", "");
  }

  public static int run(String script, String tempPrefix) throws IOException, InterruptedException {
    Path scriptFile = Files.createTempFile(tempPrefix, ".ps1");
    Files.writeString(scriptFile, script);
    scriptFile.toFile().deleteOnExit();

    Process proc = new ProcessBuilder(
        "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptFile.toString())
        .inheritIO()
        .start();
    return proc.waitFor();
  }

  public static String runWithResult(String script, String tempPrefix, Path resultFile)
      throws IOException, InterruptedException {
    int rc = run(script, tempPrefix);
    List<String> lines = Files.readAllLines(resultFile);
    if (rc != 0 || lines.isEmpty() || "CANCELLED".equals(lines.getFirst())) {
      return null;
    }
    return String.join("\n", lines);
  }
}
