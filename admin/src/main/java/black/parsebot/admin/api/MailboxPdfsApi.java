package black.parsebot.admin.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import black.parsebot.admin.server.JsonHandler;
import black.parsebot.admin.source.MailboxPdfSource;

public final class MailboxPdfsApi {

  public record EmailAttachment(int index, String filename, int size) {}

  public record EmailMessage(
      int index,
      String subject,
      String sender,
      String date,
      List<EmailAttachment> attachments
  ) {}

  public record ListResponse(boolean active, List<EmailMessage> messages, String error) {}

  public record FetchResponse(String filename, String pdfBase64) {}

  public static final class ListHandler extends JsonHandler {
    private final MailboxPdfSource source;

    public ListHandler(MailboxPdfSource source) {
      this.source = source;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      return source.list();
    }
  }

  public static final class FetchHandler extends JsonHandler {
    private final MailboxPdfSource source;

    public FetchHandler(MailboxPdfSource source) {
      this.source = source;
    }

    @Override
    protected Object handleJson(HttpExchange exchange) {
      requireMethod(exchange, "GET");
      Map<String, String> params = parseQuery(exchange.getRequestURI());
      int messageIndex = parseInt(params.get("message"), -1);
      int attachmentIndex = parseInt(params.get("attachment"), -1);
      if (messageIndex < 0 || attachmentIndex < 0) {
        throw new HttpException(400, "message and attachment query params are required");
      }
      return source.fetch(messageIndex, attachmentIndex);
    }
  }

  private static Map<String, String> parseQuery(URI uri) {
    Map<String, String> out = new HashMap<>();
    String query = uri.getRawQuery();
    if (query == null || query.isEmpty()) return out;
    for (String pair : query.split("&")) {
      int eq = pair.indexOf('=');
      if (eq < 0) {
        out.put(decode(pair), "");
      } else {
        out.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
      }
    }
    return out;
  }

  private static String decode(String s) {
    return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
  }

  private static int parseInt(String s, int fallback) {
    if (s == null) return fallback;
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private MailboxPdfsApi() {}
}
