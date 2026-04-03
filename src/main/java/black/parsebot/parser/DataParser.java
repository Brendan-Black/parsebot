package black.parsebot.parser;

import black.parsebot.model.RawData;
import black.parsebot.model.TransformedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataParser {

    private static final Logger log = LoggerFactory.getLogger(DataParser.class);

    public TransformedData parse(RawData raw) {
        log.info("Parsing: {} (source: {})", raw.getName(), raw.getSource());

        // TODO: Implement actual parsing/transformation logic.
        // This stub passes data through with a sanitized filename.
        // Throw an exception here to signal transformation failure.
        String filename = sanitizeFilename(raw.getName()) + ".out";
        return new TransformedData(filename, raw.getContent());
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
