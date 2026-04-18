package black.parsebot.parser;

import black.parsebot.model.TransformedData;
import black.parsebot.model.raw.RawMailboxData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataParser {

	private static final Logger log = LoggerFactory.getLogger(DataParser.class);

	private final ClaudeClient claudeClient;
	private final String customerCsv;
	private final String productCsv;

	public DataParser(ClaudeClient claudeClient, String customerCsv, String productCsv) {
		this.claudeClient = claudeClient;
		this.customerCsv = customerCsv;
		this.productCsv = productCsv;
	}

	/**
	 * Parses the email using default rules and product list.
	 */
	public List<TransformedData> parse(RawMailboxData raw) throws IOException, InterruptedException {
		return parse(raw, null, null);
	}

	/**
	 * Parses the email, using custom overrides if provided.
	 *
	 * @param raw              the raw email data
	 * @param customRules      custom system prompt, or null to use default
	 * @param customProductCsv custom product list CSV, or null to use default
	 */
	public List<TransformedData> parse(RawMailboxData raw, String customRules, String customProductCsv)
			throws IOException, InterruptedException {
		log.info("Parsing: {} (source: {})", raw.getName(), raw.getClass().getSimpleName());

		List<Map.Entry<String, byte[]>> pdfs;
		try {
			pdfs = raw.extractPdfAttachments();
		} catch (Exception e) {
			throw new IOException("Failed to extract PDF attachments from: " + raw.getName(), e);
		}

		if (pdfs.isEmpty()) {
			log.warn("No PDF attachments found in: {}", raw.getName());
			return List.of();
		}

		String effectiveProductCsv = (customProductCsv != null && !customProductCsv.isBlank()) ?
				customProductCsv 
				: productCsv;

		List<TransformedData> results = new ArrayList<>();

		for (Map.Entry<String, byte[]> pdf : pdfs) {
			String pdfName = pdf.getKey();
			byte[] pdfBytes = pdf.getValue();

			log.info("Sending PDF attachment '{}' to Claude", pdfName);
			String responseJson = claudeClient.parsePdf(pdfName, pdfBytes, customerCsv, effectiveProductCsv,customRules);

			String outputFilename = sanitizeFilename(pdfName) + ".json";
			results.add(new TransformedData(outputFilename, responseJson.getBytes(StandardCharsets.UTF_8)));
		}

		return results;
	}

	private String sanitizeFilename(String name) {
		if (name == null || name.isBlank()) {
			return "unnamed";
		}
		String withoutExtension = name.replaceFirst("\\.[^.]+$", "");
		return withoutExtension.replaceAll("[^a-zA-Z0-9._-]", "_");
	}
}
