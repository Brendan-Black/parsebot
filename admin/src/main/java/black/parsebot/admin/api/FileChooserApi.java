package black.parsebot.admin.api;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.source.FileChooser;

public final class FileChooserApi {

  public record FileChooserRequest(String mode, String initialPath, String title) {}

  public record FileChooserResponse(String path) {}

  public static final class Handler extends JsonHandler {
    private final FileChooser chooser;

    public Handler(FileChooser chooser) {
      this.chooser = chooser;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) throws Exception {
      requireMethod(exchange, "POST");
      FileChooserRequest req = readJson(exchange, FileChooserRequest.class);
      FileChooser.Mode mode = parseMode(req == null ? null : req.mode());
      String initial = req == null ? null : req.initialPath();
      String title = req == null ? null : req.title();
      String chosen = chooser.choose(mode, initial, title);
      return new FileChooserResponse(chosen);
    }
  }

  private static FileChooser.Mode parseMode(String raw) {
    if (raw == null) throw new JsonHandler.HttpException(400, "Missing mode");
    return switch (raw.toLowerCase()) {
      case "file" -> FileChooser.Mode.FILE;
      case "folder" -> FileChooser.Mode.FOLDER;
      default -> throw new JsonHandler.HttpException(400, "Invalid mode: " + raw);
    };
  }

  private FileChooserApi() {}
}
