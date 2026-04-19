package black.parsebot.admin.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.service.ServiceManager;

public final class ReferenceDataApi {

  public record ReferenceFile(String path, String content, String error) {}

  public record ReferenceDataResponse(
      ReferenceFile customers,
      ReferenceFile products,
      ReferenceFile priceMatrix
  ) {}

  public static final class Handler extends JsonHandler {
    private final ServiceManager serviceManager;

    public Handler(ServiceManager serviceManager) {
      this.serviceManager = serviceManager;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      Map<String, String> cfg = serviceManager.currentConfig();
      return new ReferenceDataResponse(
          load(cfg.get("csv.customers")),
          load(cfg.get("csv.products")),
          load(cfg.get("csv.pricematrix"))
      );
    }

    private static ReferenceFile load(String pathStr) {
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

  private ReferenceDataApi() {}
}
