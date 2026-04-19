package black.parsebot.admin.api;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.AdminServer;
import black.parsebot.admin.server.JsonHandler;

public final class HeartbeatApi {

  public record HeartbeatResponse(boolean ok) {}

  public static final class Handler extends JsonHandler {
    private final AdminServer server;

    public Handler(AdminServer server) {
      this.server = server;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      server.recordHeartbeat();
      return new HeartbeatResponse(true);
    }
  }

  private HeartbeatApi() {}
}
