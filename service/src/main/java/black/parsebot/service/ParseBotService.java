package black.parsebot.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.config.AppConfig;
import black.parsebot.config.CustomOverrideResolver;
import black.parsebot.model.TransformedData;
import black.parsebot.model.raw.RawMailboxData;
import black.parsebot.parser.ClaudeClient;
import black.parsebot.parser.DataParser;
import black.parsebot.reader.MailboxReader;
import black.parsebot.writer.MailboxWriter;
import black.parsebot.writer.SftpWriter;

public class ParseBotService {

	private static final Logger log = LoggerFactory.getLogger(ParseBotService.class);

	private final MailboxReader mailReader;
	private final MailboxWriter mailWriter;
	private final ClaudeClient claudeClient;
	private final SftpWriter sftpWriter;
	private final AppConfig.MailConfig mailConfig;
	private final Path customerCsvPath;
	private final Path productCsvPath;
	private final CustomOverrideResolver overrideResolver;

	public ParseBotService(AppConfig config) throws IOException {
		this.mailConfig = config.getMailConfig();
		this.mailReader = new MailboxReader(config.getMailConfig());
		this.mailWriter = new MailboxWriter(mailReader);

		this.claudeClient = new ClaudeClient(config.getClaudeApiKey());
		this.customerCsvPath = Path.of(config.getCustomerCsvPath());
		this.productCsvPath = Path.of(config.getProductCsvPath());
		this.overrideResolver = new CustomOverrideResolver(
				config.getCustomRulesDir(), config.getCustomProductListsDir());

		this.sftpWriter = new SftpWriter(config.getSftpConfig());
	}

	public void run() {
		try {
			log.info("Starting pipeline run");

			String customerCsv = Files.readString(customerCsvPath);
			String productCsv = Files.readString(productCsvPath);
			DataParser parser = new DataParser(claudeClient, customerCsv, productCsv);

			// Collect raw data from all sources
			List<RawMailboxData> rawData = new ArrayList<>();
			rawData.addAll(mailReader.read());

			if (rawData.isEmpty()) {
				log.info("No data to process");
				return;
			}

			// Process each item individually
			int successCount = 0;
			int failCount = 0;

			for (RawMailboxData email : rawData) {
				try {
					String sender = email.getSenderEmail();
					String customRules = overrideResolver.resolveRules(sender);
					String customProductList = overrideResolver.resolveProductList(sender);

					if (customRules != null || customProductList != null) {
						log.info("Using custom overrides for sender '{}'", sender);
					}

					List<TransformedData> transformed = parser.parse(email, customRules, customProductList);
					if (!transformed.isEmpty()) {
						sftpWriter.write(transformed);
					}
					onSuccess(email, mailWriter);
					successCount++;
				} catch (Exception e) {
					log.error("Failed to process: {}", email.getName(), e);
					onFailure(email, mailWriter);
					failCount++;
				}
			}

			log.info("Pipeline run complete: {} succeeded, {} failed", successCount, failCount);
		} catch (Exception e) {
			log.error("Pipeline run failed", e);
		}
	}

	public void close() {
		mailReader.close();
	}

	private void onSuccess(RawMailboxData mbd, MailboxWriter mailWriter) {
		mailWriter.moveToFolder(mbd.getSourceMessage(), mailConfig.getSuccessFolder());
	}

	private void onFailure(RawMailboxData mbd, MailboxWriter mailWriter) {
		mailWriter.moveToFolder(mbd.getSourceMessage(), mailConfig.getFailureFolder());
	}
}
