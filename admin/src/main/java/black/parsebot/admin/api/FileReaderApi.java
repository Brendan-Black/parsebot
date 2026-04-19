package black.parsebot.admin.api;

import com.sun.net.httpserver.HttpExchange;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import black.parsebot.admin.server.JsonHandler;

public final class FileReaderApi {

  public record FileReadRequest(String path) {}

  public record FileReadResponse(String filename, String base64, long sizeBytes) {}

  public static final class Handler extends JsonHandler {

    @Override
    protected Object handleJson(HttpExchange exchange) throws Exception {
      requireMethod(exchange, "POST");
      FileReadRequest req = readJson(exchange, FileReadRequest.class);
      if (req == null || req.path() == null || req.path().isBlank()) {
        throw new HttpException(400, "Missing path");
      }
      Path p = Path.of(req.path());
      if (!Files.isRegularFile(p)) {
        throw new HttpException(404, "Not a file: " + req.path());
      }
      byte[] bytes = Files.readAllBytes(p);
      return new FileReadResponse(
          p.getFileName().toString(),
          Base64.getEncoder().encodeToString(bytes),
          bytes.length);
    }
  }

  private FileReaderApi() {}
}
