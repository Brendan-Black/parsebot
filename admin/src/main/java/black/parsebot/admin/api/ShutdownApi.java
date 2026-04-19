package black.parsebot.admin.api;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.AdminServer;
import black.parsebot.admin.server.JsonHandler;

public final class ShutdownApi {

  public record ShutdownResponse(boolean ok) {}

  public static final class Handler extends JsonHandler {
    private final AdminServer server;

    public Handler(AdminServer server) {
      this.server = server;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "POST");
      new Thread(() -> {
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        server.shutdown();
      }, "admin-shutdown").start();
      return new ShutdownResponse(true);
    }
  }

  private ShutdownApi() {}
}
