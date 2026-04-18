package black.parsebot.notify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import black.parsebot.config.NotifyConfig;
import black.parsebot.event.Event;
import black.parsebot.event.EventSeverity;

public class TeamsWebhookChannel implements NotificationChannel {

  private static final Logger log = LoggerFactory.getLogger(TeamsWebhookChannel.class);

  private final String webhookUrl;
  private final HttpClient httpClient;
  private final Gson gson;

  public TeamsWebhookChannel(NotifyConfig config) {
    this.webhookUrl = config.getTeamsWebhookUrl();
    this.httpClient = HttpClient.newHttpClient();
    this.gson = new Gson();
  }

  @Override
  public void send(Event event) throws Exception {
    JsonObject payload = new JsonObject();
    payload.addProperty("@type", "MessageCard");
    payload.addProperty("@context", "http://schema.org/extensions");
    payload.addProperty("summary", "ParseBot Alert");
    payload.addProperty("themeColor", event.severity() == EventSeverity.CRITICAL ? "FF0000" : "0076D7");
    payload.addProperty("title", "ParseBot: " + event.type());

    StringBuilder text = new StringBuilder();
    text.append("**Severity:** ").append(event.severity()).append("\n\n");
    text.append("**Time:** ").append(event.timestamp()).append("\n\n");
    text.append("**Message:** ").append(event.message());
    if (event.details() != null && !event.details().isEmpty()) {
      text.append("\n\n**Details:**\n\n");
      event.details().forEach((k, v) -> text.append("- ").append(k).append(": ").append(v).append("\n"));
    }
    payload.addProperty("text", text.toString());

    String json = gson.toJson(payload);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(webhookUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 400) {
      throw new IOException("Teams webhook returned HTTP " + response.statusCode() + ": " + response.body());
    }

    log.info("Sent Teams notification for event {}", event.id());
  }
}
