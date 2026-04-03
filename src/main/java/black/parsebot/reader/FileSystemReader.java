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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSystemReader {

    private static final Logger log = LoggerFactory.getLogger(FileSystemReader.class);

    private final Path inputDirectory;
    private final String filePattern;
    private final Path successDirectory;
    private final Path failureDirectory;
    private final Map<String, Path> sourceFiles = new HashMap<>();

    public FileSystemReader(AppConfig config) {
        this.inputDirectory = Path.of(config.getInputDirectory());
        this.filePattern = config.getFilePattern();
        this.successDirectory = inputDirectory.resolve("success");
        this.failureDirectory = inputDirectory.resolve("failed");
    }

    public List<RawFileData> read() {
        List<RawFileData> results = new ArrayList<>();
        sourceFiles.clear();

        if (!Files.isDirectory(inputDirectory)) {
            log.warn("Input directory does not exist: {}", inputDirectory);
            return results;
        }

        try {
            Files.createDirectories(successDirectory);
            Files.createDirectories(failureDirectory);
        } catch (IOException e) {
            log.error("Failed to create output directories", e);
            return results;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDirectory, filePattern)) {
            for (Path file : stream) {
                log.info("Reading file: {}", file.getFileName());
                byte[] content = Files.readAllBytes(file);
                String name = file.getFileName().toString();
                sourceFiles.put(name, file);
                results.add(new RawFileData(name, content));
            }
        } catch (IOException e) {
            log.error("Error reading from input directory: {}", inputDirectory, e);
        }

        log.info("Read {} file(s) from {}", results.size(), inputDirectory);
        return results;
    }

    public void moveToSuccess(String name) {
        moveFile(name, successDirectory);
    }

    public void moveToFailure(String name) {
        moveFile(name, failureDirectory);
    }

    private void moveFile(String name, Path targetDirectory) {
        Path source = sourceFiles.get(name);
        if (source == null) {
            log.warn("No source file tracked for: {}", name);
            return;
        }

        try {
            Files.move(source, targetDirectory.resolve(source.getFileName()));
            log.info("Moved {} to {}", name, targetDirectory);
        } catch (IOException e) {
            log.error("Failed to move file {} to {}", name, targetDirectory, e);
        }
    }
}
