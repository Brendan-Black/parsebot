package black.parsebot.admin.update;

import java.util.List;

public final class DemoUpdateChecker implements UpdateChecker {

  @Override
  public String currentVersion() {
    return VersionInfo.current();
  }

  @Override
  public List<ReleaseInfo> listReleases() {
    String current = currentVersion();
    return List.of(
        new ReleaseInfo("v" + current, "Current (demo)", "2026-04-01T00:00:00Z",
            "https://example.invalid/current.zip", 12_345_678L,
            "This is demo mode — no real releases are listed.",
            true, false, false));
  }
}
