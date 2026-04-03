package black.parsebot.reader;

import black.parsebot.config.AppConfig;
import black.parsebot.model.raw.RawFileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileSystemReader {

    private static final Logger log = LoggerFactory.getLogger(FileSystemReader.class);

    private final Path inputDirectory;
    private final String filePattern;

    public FileSystemReader(AppConfig config) {
        this.inputDirectory = Path.of(config.getFileSystemConfig().getCenter());
        this.filePattern = config.getFileSystemConfig().getFilePattern();
    }

    public List<RawFileData> read() {
        List<RawFileData> results = new ArrayList<>();

        if (!Files.isDirectory(inputDirectory)) {
            log.warn("Input directory does not exist: {}", inputDirectory);
            return results;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDirectory, filePattern)) {
            for (Path file : stream) {
                log.info("Reading file: {}", file.getFileName());
                byte[] content = Files.readAllBytes(file);
                String name = file.getFileName().toString();
                results.add(new RawFileData(name, content));
            }
        } catch (IOException e) {
            log.error("Error reading from input directory: {}", inputDirectory, e);
        }

        log.info("Read {} file(s) from {}", results.size(), inputDirectory);
        return results;
    }
}
