package black.parsebot.admin.service;

import java.util.Map;

public interface ServiceManager {

  Map<String, String> currentConfig();

  InstallResult install(Map<String, String> values, boolean dryRun);

  int uninstall();

  ServiceStatus status();

  default String statusText() {
    ServiceStatus s = status();
    if (!s.installed()) return "Not installed.";
    return s.running() ? "Installed and running." : "Installed (not running).";
  }

  record InstallResult(boolean success, String message, String binPath) {}

  record ServiceStatus(boolean installed, boolean running, String detail) {}
}
