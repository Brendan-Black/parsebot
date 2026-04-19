package black.parsebot.admin.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public abstract class JsonHandler implements HttpHandler {

  protected static final Gson GSON = new GsonBuilder()
      .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, t, ctx) ->
          new JsonPrimitive(src.toString()))
      .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, t, ctx) ->
          Instant.parse(json.getAsString()))
      .create();

  @Override
  public final void handle(HttpExchange exchange) throws IOException {
    try {
      Object body = handleJson(exchange);
      writeJson(exchange, 200, body);
    } catch (HttpException e) {
      writeJson(exchange, e.status(), new ErrorResponse(e.getMessage()));
    } catch (Exception e) {
      writeJson(exchange, 500, new ErrorResponse(e.getClass().getSimpleName() + ": " + e.getMessage()));
    } finally {
      exchange.close();
    }
  }

  protected abstract Object handleJson(HttpExchange exchange) throws Exception;

  protected static <T> T readJson(HttpExchange exchange, Type type) throws IOException {
    try (Reader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
      return GSON.fromJson(reader, type);
    }
  }

  protected static void requireMethod(HttpExchange exchange, String method) {
    if (!method.equals(exchange.getRequestMethod())) {
      throw new HttpException(405, "Method not allowed: " + exchange.getRequestMethod());
    }
  }

  private static void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
    byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }

  public static final class HttpException extends RuntimeException {
    private final int status;

    public HttpException(int status, String message) {
      super(message);
      this.status = status;
    }

    public int status() {
      return status;
    }
  }

  public record ErrorResponse(String error) {}
}
