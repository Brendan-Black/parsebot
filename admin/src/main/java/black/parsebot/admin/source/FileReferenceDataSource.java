package black.parsebot.admin.source;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import black.parsebot.admin.api.ReferenceDataApi.ReferenceDataResponse;
import black.parsebot.admin.api.ReferenceDataApi.ReferenceFile;
import black.parsebot.admin.service.ServiceManager;
import black.parsebot.config.ConfigKey;

public final class FileReferenceDataSource implements ReferenceDataSource {

  private final ServiceManager serviceManager;

  public FileReferenceDataSource(ServiceManager serviceManager) {
    this.serviceManager = serviceManager;
  }

  @Override
  public ReferenceDataResponse load() {
    Map<String, String> cfg = serviceManager.currentConfig();
    return new ReferenceDataResponse(
        loadFile(cfg.get(ConfigKey.CSV_CUSTOMERS)),
        loadFile(cfg.get(ConfigKey.CSV_PRODUCTS)),
        loadFile(cfg.get(ConfigKey.CSV_PRICEMATRIX))
    );
  }

  private static ReferenceFile loadFile(String pathStr) {
    if (pathStr == null || pathStr.isBlank()) return null;
    Path path = Path.of(pathStr);
    if (!Files.isRegularFile(path)) {
      return new ReferenceFile(pathStr, null, "File not found");
    }
    try {
      return new ReferenceFile(pathStr, Files.readString(path), null);
    } catch (Exception e) {
      return new ReferenceFile(pathStr, null, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }
}
