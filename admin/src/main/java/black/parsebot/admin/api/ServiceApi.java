package black.parsebot.admin.api;

import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.service.ServiceManager;

public final class ServiceApi {

  public record InstallRequest(Map<String, String> values, boolean dryRun) {}

  public record UninstallResponse(boolean success, int exitCode) {}

  public static final class InstallHandler extends JsonHandler {
    private final ServiceManager serviceManager;

    public InstallHandler(ServiceManager serviceManager) {
      this.serviceManager = serviceManager;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) throws Exception {
      requireMethod(exchange, "POST");
      InstallRequest req = readJson(exchange, InstallRequest.class);
      if (req == null || req.values == null) {
        throw new HttpException(400, "Missing values");
      }
      return serviceManager.install(req.values, req.dryRun);
    }
  }

  public static final class UninstallHandler extends JsonHandler {
    private final ServiceManager serviceManager;

    public UninstallHandler(ServiceManager serviceManager) {
      this.serviceManager = serviceManager;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "POST");
      int rc = serviceManager.uninstall();
      return new UninstallResponse(rc == 0, rc);
    }
  }

  public static final class StatusHandler extends JsonHandler {
    private final ServiceManager serviceManager;

    public StatusHandler(ServiceManager serviceManager) {
      this.serviceManager = serviceManager;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      return serviceManager.status();
    }
  }

  private ServiceApi() {}
}
