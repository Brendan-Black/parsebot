package black.parsebot.admin.api;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.source.ReferenceDataSource;

public final class ReferenceDataApi {

  public record ReferenceFile(String path, String content, String error) {}

  public record ReferenceDataResponse(
      ReferenceFile customers,
      ReferenceFile products,
      ReferenceFile priceMatrix
  ) {}

  public static final class Handler extends JsonHandler {
    private final ReferenceDataSource source;

    public Handler(ReferenceDataSource source) {
      this.source = source;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      return source.load();
    }
  }

  private ReferenceDataApi() {}
}
