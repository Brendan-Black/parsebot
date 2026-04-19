package black.parsebot.admin.api;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.AdminContext;
import black.parsebot.admin.server.JsonHandler;

public final class ModeApi {

  public record ModeResponse(boolean demo) {}

  public static final class Handler extends JsonHandler {
    private final AdminContext context;

    public Handler(AdminContext context) {
      this.context = context;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      return new ModeResponse(context.demoMode());
    }
  }

  private ModeApi() {}
}
