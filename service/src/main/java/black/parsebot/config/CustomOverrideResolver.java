package black.parsebot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves per-sender override files from custom_rules and custom_productlists directories.
 * <p>
 * Lookup order for a sender like "person@place.com":
 * <ol>
 *   <li>Exact match: {@code person@place.com}</li>
 *   <li>Domain wildcard: {@code *@place.com}</li>
 *   <li>Falls back to null (use default)</li>
 * </ol>
 */
public class CustomOverrideResolver {

	private static final Logger log = LoggerFactory.getLogger(CustomOverrideResolver.class);

	private final Path customRulesDir;
	private final Path customProductListsDir;

	public CustomOverrideResolver(String customRulesDir, String customProductListsDir) {
		this.customRulesDir = Path.of(customRulesDir);
		this.customProductListsDir = Path.of(customProductListsDir);
	}

	/**
	 * Returns the custom rules content for the given sender, or null if no override exists.
	 */
	public String resolveRules(String senderEmail) {
		return resolveFile(customRulesDir, senderEmail, "rules");
	}

	/**
	 * Returns the custom product list content for the given sender, or null if no override exists.
	 */
	public String resolveProductList(String senderEmail) {
		return resolveFile(customProductListsDir, senderEmail, "product list");
	}

	private String resolveFile(Path dir, String senderEmail, String label) {
		if (senderEmail == null || senderEmail.isBlank()) {
			return null;
		}
		if (!Files.isDirectory(dir)) {
			return null;
		}

		// Try exact match first
		Path exact = dir.resolve(senderEmail);
		if (Files.isRegularFile(exact)) {
			log.info("Using custom {} for sender '{}': {}", label, senderEmail, exact);
			return readFile(exact);
		}

		// Try domain wildcard
		int atIndex = senderEmail.indexOf('@');
		if (atIndex > 0) {
			String domain = senderEmail.substring(atIndex + 1);
			Path wildcard = dir.resolve("*@" + domain);
			if (Files.isRegularFile(wildcard)) {
				log.info("Using custom {} for sender '{}' (wildcard match): {}", label, senderEmail, wildcard);
				return readFile(wildcard);
			}
		}

		return null;
	}

	private String readFile(Path path) {
		try {
			return Files.readString(path);
		} catch (IOException e) {
			log.error("Failed to read custom override file: {}", path, e);
			return null;
		}
	}
}
