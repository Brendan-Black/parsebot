package black.parsebot.admin.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import black.parsebot.admin.service.ServiceManager;

public final class DemoServiceManager implements ServiceManager {

  private final Map<String, String> config = new LinkedHashMap<>(DemoData.serviceConfig());

  @Override
  public Map<String, String> currentConfig() {
    return Map.copyOf(config);
  }

  @Override
  public InstallResult install(Map<String, String> values, boolean dryRun) {
    if (!dryRun) {
      config.clear();
      config.putAll(values);
    }
    return new InstallResult(true,
        dryRun ? "(demo) Would install with supplied values." : "(demo) Config updated in-memory.",
        "(demo) no binary path");
  }

  @Override
  public int uninstall() {
    return 0;
  }

  @Override
  public ServiceStatus status() {
    return new ServiceStatus(true, true, "(demo) Installed and running.");
  }
}
