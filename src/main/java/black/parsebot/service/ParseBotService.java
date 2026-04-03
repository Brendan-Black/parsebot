package black.parsebot.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.config.AppConfig;
import black.parsebot.model.TransformedData;
import black.parsebot.model.raw.RawMailboxData;
import black.parsebot.parser.DataParser;
import black.parsebot.reader.MailboxReader;
import black.parsebot.writer.MailboxWriter;
import black.parsebot.writer.SftpWriter;

public class ParseBotService {

	private static final Logger log = LoggerFactory.getLogger(ParseBotService.class);

	private final MailboxReader mailReader;
	private final MailboxWriter mailWriter;
	private final DataParser parser;
	private final SftpWriter sftpWriter;
	private final AppConfig.MailConfig mailConfig;

	public ParseBotService(AppConfig config) {
		this.mailConfig = config.getMailConfig();
		this.mailReader = new MailboxReader(config.getMailConfig());
		this.mailWriter = new MailboxWriter(mailReader);
		this.parser = new DataParser();
		this.sftpWriter = new SftpWriter(config.getSftpConfig());
	}

	public void run() {
		try {
			log.info("Starting pipeline run");

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
					TransformedData transformed = parser.parse(email);
					sftpWriter.write(List.of(transformed));
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
