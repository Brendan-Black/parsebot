package black.parsebot.admin.service;

import static black.parsebot.admin.config.ConfigSchema.DESCRIPTION;
import static black.parsebot.admin.config.ConfigSchema.DISPLAY_NAME;
import static black.parsebot.admin.config.ConfigSchema.SERVICE_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WindowsServiceManager implements ServiceManager {

  private final Path exePath;

  public WindowsServiceManager(Path exePath) {
    this.exePath = exePath;
  }

  @Override
  public Map<String, String> currentConfig() {
    String raw = runCapture("qc", SERVICE_NAME);
    if (raw == null) return Collections.emptyMap();
    Map<String, String> config = new LinkedHashMap<>();
    Pattern pattern = Pattern.compile("-Dparsebot\\.([^=]+)=([^\"]*)");
    Matcher matcher = pattern.matcher(raw);
    while (matcher.find()) {
      config.put(matcher.group(1), matcher.group(2).trim());
    }
    return config;
  }

  @Override
  public InstallResult install(Map<String, String> values, boolean dryRun) {
    if (!dryRun && !Files.isRegularFile(exePath)) {
      return new InstallResult(false, "Service executable not found: " + exePath, null);
    }

    StringBuilder binPath = new StringBuilder();
    binPath.append('"').append(exePath.toAbsolutePath()).append('"');
    for (var entry : values.entrySet()) {
      String val = entry.getValue();
      if (val != null && !val.isEmpty()) {
        binPath.append(" \"-Dparsebot.").append(entry.getKey()).append('=').append(val).append('"');
      }
    }

    if (dryRun) {
      return new InstallResult(true,
          "[dry-run] Would run: sc create " + SERVICE_NAME + " binPath= " + binPath,
          binPath.toString());
    }

    int rc = sc("create", SERVICE_NAME,
        "binPath=", binPath.toString(),
        "DisplayName=", DISPLAY_NAME,
        "start=", "auto");
    if (rc != 0) {
      return new InstallResult(false, "Failed to create service (exit " + rc + ").", binPath.toString());
    }
    sc("description", SERVICE_NAME, DESCRIPTION);
    sc("failure", SERVICE_NAME, "reset=", "86400", "actions=", "restart/5000/restart/10000/restart/30000");

    int startRc = sc("start", SERVICE_NAME);
    if (startRc != 0) {
      return new InstallResult(true,
          "Service installed; failed to start (exit " + startRc + "). Start manually: sc start " + SERVICE_NAME,
          binPath.toString());
    }
    return new InstallResult(true, SERVICE_NAME + " installed and running.", binPath.toString());
  }

  @Override
  public int uninstall() {
    sc("stop", SERVICE_NAME);
    return sc("delete", SERVICE_NAME);
  }

  @Override
  public ServiceStatus status() {
    String qc = runCapture("qc", SERVICE_NAME);
    if (qc == null) return new ServiceStatus(false, false, "Not installed");
    String query = runCapture("query", SERVICE_NAME);
    boolean running = query != null && query.contains("RUNNING");
    return new ServiceStatus(true, running, query != null ? query : qc);
  }

  private static int sc(String... args) {
    String[] command = new String[args.length + 1];
    command[0] = "sc.exe";
    System.arraycopy(args, 0, command, 1, args.length);
    try {
      return new ProcessBuilder(command).inheritIO().start().waitFor();
    } catch (IOException | InterruptedException e) {
      System.err.println("Failed to run sc.exe: " + e.getMessage());
      return 1;
    }
  }

  private static String runCapture(String... args) {
    String[] command = new String[args.length + 1];
    command[0] = "sc.exe";
    System.arraycopy(args, 0, command, 1, args.length);
    try {
      Process proc = new ProcessBuilder(command).redirectErrorStream(true).start();
      StringBuilder out = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) out.append(line).append('\n');
      }
      int rc = proc.waitFor();
      return rc == 0 ? out.toString() : null;
    } catch (IOException | InterruptedException e) {
      return null;
    }
  }
}
