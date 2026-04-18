package black.parsebot.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class ClaudeClient {

	private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

	private static final String API_URL = "https://api.anthropic.com/v1/messages";
	private static final String MODEL = "claude-sonnet-4-20250514";
	private static final String ANTHROPIC_VERSION = "2023-06-01";

	private static final String SYSTEM_PROMPT = """
			You are an order-document parser. You will receive a PDF of a purchase order \
			along with a customer list (CSV) and a product catalog (CSV).

			Your job:
			1. Read the PDF and identify the customer and the line-item products.
			2. Best-effort match the customer against the customer CSV by name, address, \
			or any other identifying information.
			3. Best-effort match each line-item product against the product CSV by name, \
			SKU, description, or any other identifying information.
			4. Only call the "match" tool if you have HIGH confidence in the customer match \
			AND high confidence in EVERY product match. If any match — customer or any \
			individual product — is less than high confidence, call "reject" instead.
			5. Also reject if the document is not a parseable order, or you cannot identify \
			a customer or any products.

			Products in the PDF may be poorly formatted, abbreviated, or misspelled. \
			Use fuzzy matching and your best judgement, but only accept matches you are \
			confident are correct.
			""";

	private static final String TOOLS = """
		[
				{
						"name": "match",
						"description": "A successfully parsed order where the customer and ALL products were matched with high confidence.",
						"input_schema": {
								"type": "object",
								"properties": {
										"customer_number": {
												"type": "string",
												"description": "The matched customer number from the customer CSV."
										},
										"customer_name": {
												"type": "string",
												"description": "The customer name as it appears in the PDF."
										},
										"items": {
												"type": "array",
												"description": "The line-item products extracted from the order.",
												"items": {
														"type": "object",
														"properties": {
																"product_id": {
																		"type": "string",
																		"description": "The matched product ID from the product CSV."
																},
																"product_name": {
																		"type": "string",
																		"description": "The product name as it appears in the PDF."
																},
																"quantity": {
																		"type": "number",
																		"description": "The quantity ordered."
																},
																"unit_price": {
																		"type": "number",
																		"description": "The unit price if present in the PDF, or 0 if absent."
																}
														},
														"required": ["product_id", "product_name", "quantity"]
												}
										}
								},
								"required": ["customer_number", "customer_name", "items"]
						}
				},
				{
						"name": "reject",
						"description": "The document could not be parsed as a valid order.",
						"input_schema": {
								"type": "object",
								"properties": {
										"reason": {
												"type": "string",
												"description": "Why the document was rejected."
										}
								},
								"required": ["reason"]
						}
				}
		]
		""";

	private final String apiKey;
	private final HttpClient httpClient;

	public ClaudeClient(String apiKey) {
		this.apiKey = apiKey;
		this.httpClient = HttpClient.newHttpClient();
	}

	/**
	 * Sends a PDF order document to Claude for parsing against known customers and products.
	 *
	 * @param filename    the name of the PDF file
	 * @param pdfBytes    the raw PDF bytes
	 * @param customerCsv the full customer list as CSV text
	 * @param productCsv  the full product catalog as CSV text
	 * @return the raw JSON response body from the Messages API
	 */
	public String parsePdf(String filename, byte[] pdfBytes, String customerCsv, String productCsv)
			throws IOException, InterruptedException {
		return parsePdf(filename, pdfBytes, customerCsv, productCsv, null);
	}

	/**
	 * Sends a PDF order document to Claude for parsing, with an optional custom system prompt.
	 *
	 * @param filename       the name of the PDF file
	 * @param pdfBytes       the raw PDF bytes
	 * @param customerCsv    the full customer list as CSV text
	 * @param productCsv     the full product catalog as CSV text
	 * @param customRules    custom system prompt to use instead of the default, or null for default
	 * @return the raw JSON response body from the Messages API
	 */
	public String parsePdf(String filename, byte[] pdfBytes, String customerCsv, String productCsv,
						   String customRules)
			throws IOException, InterruptedException {
		String base64Content = Base64.getEncoder().encodeToString(pdfBytes);

		String systemPrompt = (customRules != null && !customRules.isBlank()) ? customRules : SYSTEM_PROMPT;
		String requestBody = buildRequestBody(filename, base64Content, customerCsv, productCsv, systemPrompt);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(API_URL))
				.header("Content-Type", "application/json")
				.header("x-api-key", apiKey)
				.header("anthropic-version", ANTHROPIC_VERSION)
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

		log.info("Sending PDF '{}' ({} bytes) to Claude for parsing", filename, pdfBytes.length);

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			log.error("Claude API returned status {}: {}", response.statusCode(), response.body());
			throw new IOException("Claude API error (HTTP " + response.statusCode() + "): " + response.body());
		}

		log.info("Received response for '{}'", filename);
		return response.body();
	}

	private String buildRequestBody(String filename, String base64Pdf, String customerCsv, String productCsv,
									String systemPrompt) {
		String safeFilename = jsonEscape(filename);
		String safeCustomerCsv = jsonEscape(customerCsv);
		String safeProductCsv = jsonEscape(productCsv);

		return """
			{
					"model": "%s",
					"max_tokens": 4096,
					"system": %s,
					"tools": %s,
					"tool_choice": {"type": "any"},
					"messages": [
							{
									"role": "user",
									"content": [
											{
													"type": "document",
													"source": {
															"type": "base64",
															"media_type": "application/pdf",
															"data": "%s"
													}
											},
											{
													"type": "text",
													"text": "Parse the attached PDF order: %s\\n\\n--- CUSTOMER LIST (CSV) ---\\n%s\\n\\n--- PRODUCT CATALOG (CSV) ---\\n%s"
											}
									]
							}
					]
			}
			""".formatted(MODEL, jsonString(systemPrompt), TOOLS, base64Pdf,
				safeFilename, safeCustomerCsv, safeProductCsv);
	}

	/**
	 * Escapes a string for safe embedding inside a JSON string value.
	 */
	private static String jsonEscape(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	/**
	 * Wraps a plain string as a JSON-encoded string literal (with escaping).
	 */
	private static String jsonString(String value) {
		return "\"" + jsonEscape(value) + "\"";
	}
}
