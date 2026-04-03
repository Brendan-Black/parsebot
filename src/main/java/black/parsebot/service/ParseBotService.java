package black.parsebot.service;

import black.parsebot.config.AppConfig;
import black.parsebot.model.RawData;
import black.parsebot.model.TransformedData;
import black.parsebot.parser.DataParser;
import black.parsebot.reader.FileSystemReader;
import black.parsebot.reader.MailboxReader;
import black.parsebot.writer.SftpWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ParseBotService {

    private static final Logger log = LoggerFactory.getLogger(ParseBotService.class);

    private final FileSystemReader fileReader;
    private final MailboxReader mailReader;
    private final DataParser parser;
    private final SftpWriter writer;
    private final AppConfig config;

    public ParseBotService(AppConfig config) {
        this.config = config;
        this.fileReader = new FileSystemReader(config);
        this.mailReader = new MailboxReader(config);
        this.parser = new DataParser();
        this.writer = new SftpWriter(config);
    }

    public void run() {
        try {
            log.info("Starting pipeline run");

            // Collect raw data from all sources
            List<RawData> rawData = new ArrayList<>();
            rawData.addAll(fileReader.read());
            rawData.addAll(mailReader.read());

            if (rawData.isEmpty()) {
                log.info("No data to process");
                return;
            }

            // Process each item individually
            int successCount = 0;
            int failCount = 0;

            for (RawData raw : rawData) {
                try {
                    TransformedData transformed = parser.parse(raw);
                    writer.write(List.of(transformed));
                    onSuccess(raw);
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to process: {}", raw.getName(), e);
                    onFailure(raw);
                    failCount++;
                }
            }

            log.info("Pipeline run complete: {} succeeded, {} failed", successCount, failCount);
        } catch (Exception e) {
            log.error("Pipeline run failed", e);
        } finally {
            mailReader.close();
        }
    }

    private void onSuccess(RawData raw) {
        switch (raw.getSource()) {
            case FILE_SYSTEM -> fileReader.moveToSuccess(raw.getName());
            case MAILBOX -> mailReader.moveToFolder(raw.getSourceMessage(), config.getMailSuccessFolder());
        }
    }

    private void onFailure(RawData raw) {
        switch (raw.getSource()) {
            case FILE_SYSTEM -> fileReader.moveToFailure(raw.getName());
            case MAILBOX -> mailReader.moveToFolder(raw.getSourceMessage(), config.getMailFailureFolder());
        }
    }
}
