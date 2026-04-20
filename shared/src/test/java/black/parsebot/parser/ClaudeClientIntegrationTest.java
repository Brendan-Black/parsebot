package black.parsebot.parser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import black.parsebot.event.Event;
import black.parsebot.event.EventPublisher;
import black.parsebot.event.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeClientIntegrationTest {

  private static final byte[] MINIMAL_PDF =
      "%PDF-1.4\n1 0 obj<</Type/Page>>endobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

  private HttpServer server;
  private HttpClient httpClient;
  private String baseUrl;
  private List<Event> events;
  private EventPublisher eventPublisher;
  private PdfValidator validator;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    int port = server.getAddress().getPort();
    baseUrl = "http://127.0.0.1:" + port + "/v1/messages";
    httpClient = HttpClient.newHttpClient();
    events = new ArrayList<>();
    eventPublisher = events::add;
    validator = new DefaultPdfValidator(
        DefaultPdfValidator.DEFAULT_MAX_BYTES, DefaultPdfValidator.DEFAULT_MAX_PAGES);
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
    httpClient.close();
  }

  @Test
  void sendsAuthHeadersAndReturnsBodyOnSuccess() throws Exception {
    AtomicReference<String> capturedApiKey = new AtomicReference<>();
    AtomicReference<String> capturedAnthropicVersion = new AtomicReference<>();
    AtomicReference<String> capturedBody = new AtomicReference<>();
    String responseBody = "{\"content\":[{\"type\":\"tool_use\",\"name\":\"match\"}]}";

    server.createContext("/v1/messages", exchange -> {
      capturedApiKey.set(exchange.getRequestHeaders().getFirst("x-api-key"));
      capturedAnthropicVersion.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
      capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      writeResponse(exchange, 200, responseBody);
    });
    server.start();

    ClaudeClient client = new ClaudeClient(httpClient, baseUrl, "test-key", eventPublisher, validator);
    String result = client.parsePdf("order.pdf", MINIMAL_PDF, "cust,csv\n", "prod,csv\n");

    assertEquals(responseBody, result);
    assertEquals("test-key", capturedApiKey.get());
    assertEquals("2023-06-01", capturedAnthropicVersion.get());
    assertNotNull(capturedBody.get());
    assertTrue(capturedBody.get().contains("\"model\""));
    assertTrue(capturedBody.get().contains("\"tools\""));
    assertTrue(events.isEmpty(), "no events should be published on success");
  }

  @Test
  void customRulesOverrideSystemPrompt() throws Exception {
    AtomicReference<String> capturedBody = new AtomicReference<>();
    server.createContext("/v1/messages", exchange -> {
      capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      writeResponse(exchange, 200, "{}");
    });
    server.start();

    ClaudeClient client = new ClaudeClient(httpClient, baseUrl, "k", eventPublisher, validator);
    client.parsePdf("order.pdf", MINIMAL_PDF, "", "", "CUSTOM_RULES_MARKER");

    assertTrue(capturedBody.get().contains("CUSTOM_RULES_MARKER"),
        "custom rules should replace the default system prompt");
  }

  @Test
  void throwsAndPublishesEventOnNon200Status() throws Exception {
    server.createContext("/v1/messages", exchange -> writeResponse(exchange, 500, "boom"));
    server.start();

    ClaudeClient client = new ClaudeClient(httpClient, baseUrl, "k", eventPublisher, validator);
    IOException ex = assertThrows(IOException.class,
        () -> client.parsePdf("order.pdf", MINIMAL_PDF, "", ""));
    assertTrue(ex.getMessage().contains("500"));
    assertEquals(1, events.size());
    assertEquals(EventType.CLAUDE_API_FAILED, events.get(0).type());
  }

  @Test
  void rejectsInvalidPdfBeforeCallingApi() {
    // server has no handler registered — any HTTP call would fail, so this test
    // proves the PDF validation short-circuits before the network call.
    server.start();

    ClaudeClient client = new ClaudeClient(httpClient, baseUrl, "k", eventPublisher, validator);
    assertThrows(PdfValidationException.class,
        () -> client.parsePdf("not-a-pdf.pdf", "hello".getBytes(), "", ""));
    assertEquals(1, events.size());
    assertEquals(EventType.PDF_VALIDATION_FAILED, events.get(0).type());
  }

  private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
