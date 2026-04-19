package black.parsebot.admin.api;

import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.AdminServer;
import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.update.ReleaseInfo;
import black.parsebot.admin.update.UpdateApplier;
import black.parsebot.admin.update.UpdateChecker;

public final class UpdateApi {

  public record CheckResponse(String currentVersion, List<ReleaseInfo> releases) {}

  public record ApplyRequest(String tag) {}

  public record ApplyResponse(boolean success, String message) {}

  public static final class CheckHandler extends JsonHandler {
    private final UpdateChecker checker;

    public CheckHandler(UpdateChecker checker) {
      this.checker = checker;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) throws Exception {
      requireMethod(exchange, "GET");
      return new CheckResponse(checker.currentVersion(), checker.listReleases());
    }
  }

  public static final class ApplyHandler extends JsonHandler {
    private final UpdateChecker checker;
    private final UpdateApplier applier;
    private final AdminServer server;

    public ApplyHandler(UpdateChecker checker, UpdateApplier applier, AdminServer server) {
      this.checker = checker;
      this.applier = applier;
      this.server = server;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) throws Exception {
      requireMethod(exchange, "POST");
      ApplyRequest req = readJson(exchange, ApplyRequest.class);
      if (req == null || req.tag == null || req.tag.isBlank()) {
        throw new HttpException(400, "Missing tag");
      }
      ReleaseInfo match = null;
      for (ReleaseInfo r : checker.listReleases()) {
        if (req.tag.equals(r.tag())) { match = r; break; }
      }
      if (match == null) {
        throw new HttpException(404, "Release not found: " + req.tag);
      }
      UpdateApplier.ApplyResult result = applier.apply(match);
      if (result.success()) {
        new Thread(() -> {
          try { Thread.sleep(500); } catch (InterruptedException ignored) {}
          server.shutdown();
        }, "admin-shutdown-for-update").start();
      }
      return new ApplyResponse(result.success(), result.message());
    }
  }

  private UpdateApi() {}
}
