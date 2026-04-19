package black.parsebot.admin.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

final class StaticHandler implements HttpHandler {

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      String path = exchange.getRequestURI().getPath();
      if ("/".equals(path) || path.isEmpty()) path = "/index.html";

      URL res = getClass().getResource("/web" + path);
      if (res == null && !path.contains(".")) {
        res = getClass().getResource("/web/index.html");
      }
      if (res == null) {
        send(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes());
        return;
      }

      byte[] body;
      try (InputStream in = res.openStream()) {
        body = in.readAllBytes();
      }
      send(exchange, 200, mimeType(path), body);
    } finally {
      exchange.close();
    }
  }

  private static String mimeType(String path) {
    if (path.endsWith(".html")) return "text/html; charset=utf-8";
    if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
    if (path.endsWith(".css")) return "text/css; charset=utf-8";
    if (path.endsWith(".json")) return "application/json; charset=utf-8";
    if (path.endsWith(".svg")) return "image/svg+xml";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".ico")) return "image/x-icon";
    if (path.endsWith(".woff2")) return "font/woff2";
    return "application/octet-stream";
  }

  private static void send(HttpExchange exchange, int code, String type, byte[] body) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.sendResponseHeaders(code, body.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(body);
    }
  }
}
