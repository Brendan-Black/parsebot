package black.parsebot.admin.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class WindowsUpdateApplier implements UpdateApplier {

  private static final String HELPER_RESOURCE = "/apply-update.ps1";
  private static final List<String> EXPECTED_ITEMS = List.of("admin.exe", "service.exe", "app", "runtime");

  private final Path installDir;
  private final String serviceName;
  private final String adminExeName;

  public WindowsUpdateApplier(Path installDir, String serviceName, String adminExeName) {
    this.installDir = installDir;
    this.serviceName = serviceName;
    this.adminExeName = adminExeName;
  }

  @Override
  public ApplyResult apply(ReleaseInfo release) throws Exception {
    Path tempRoot = Files.createTempDirectory("parsebot-update-");
    Path zipFile = tempRoot.resolve("ParseBot.zip");
    Path stageDir = tempRoot.resolve("staged");
    Files.createDirectories(stageDir);

    downloadZip(release.zipAssetUrl(), zipFile);
    unzip(zipFile, stageDir);
    Path stagedRoot = findStagedRoot(stageDir);
    validateStagedLayout(stagedRoot);

    Path helper = tempRoot.resolve("apply-update.ps1");
    extractHelperScript(helper);

    Path logFile = tempRoot.resolve("apply-update.log");
    long pid = ProcessHandle.current().pid();

    ProcessBuilder pb = new ProcessBuilder(
        "cmd", "/c", "start", "",
        "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", helper.toString(),
        "-AdminPid", String.valueOf(pid),
        "-InstallDir", installDir.toString(),
        "-StagedRoot", stagedRoot.toString(),
        "-ServiceName", serviceName,
        "-AdminExe", adminExeName,
        "-LogFile", logFile.toString());
    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    pb.redirectError(ProcessBuilder.Redirect.DISCARD);
    pb.start();

    return new ApplyResult(true,
        "Update " + release.tag() + " staged. Admin will restart when complete.");
  }

  private void downloadZip(String url, Path dest) throws Exception {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();
    HttpRequest req = HttpRequest.newBuilder(URI.create(url))
        .header("Accept", "application/octet-stream")
        .header("Authorization", "Bearer " + GitHubConfig.pat())
        .header("X-GitHub-Api-Version", "2022-11-28")
        .timeout(Duration.ofMinutes(10))
        .GET()
        .build();
    HttpResponse<Path> res = client.send(req, HttpResponse.BodyHandlers.ofFile(dest));
    if (res.statusCode() / 100 != 2) {
      throw new RuntimeException("Download failed: HTTP " + res.statusCode());
    }
  }

  private void unzip(Path zip, Path destRoot) throws IOException {
    try (InputStream fis = Files.newInputStream(zip);
         ZipInputStream zis = new ZipInputStream(fis)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        Path resolved = destRoot.resolve(entry.getName()).normalize();
        if (!resolved.startsWith(destRoot)) {
          throw new IOException("Zip entry escapes destination: " + entry.getName());
        }
        if (entry.isDirectory()) {
          Files.createDirectories(resolved);
        } else {
          Files.createDirectories(resolved.getParent());
          Files.copy(zis, resolved, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  private Path findStagedRoot(Path stageDir) throws IOException {
    try (var stream = Files.list(stageDir)) {
      var items = stream.toList();
      if (items.size() == 1 && Files.isDirectory(items.get(0))) {
        return items.get(0);
      }
    }
    return stageDir;
  }

  private void validateStagedLayout(Path stagedRoot) throws IOException {
    for (String item : EXPECTED_ITEMS) {
      if (!Files.exists(stagedRoot.resolve(item))) {
        throw new IOException("Downloaded archive missing expected item: " + item);
      }
    }
  }

  private void extractHelperScript(Path dest) throws IOException {
    try (InputStream in = WindowsUpdateApplier.class.getResourceAsStream(HELPER_RESOURCE)) {
      if (in == null) throw new IOException("Helper script resource not found: " + HELPER_RESOURCE);
      Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
