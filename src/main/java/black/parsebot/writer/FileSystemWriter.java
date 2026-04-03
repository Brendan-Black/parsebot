package black.parsebot.writer;

import black.parsebot.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemWriter {

    private static final Logger log = LoggerFactory.getLogger(FileSystemWriter.class);

    private final Path inputDirectory;
    private final Path successDirectory;
    private final Path failureDirectory;

    public FileSystemWriter(AppConfig config) {
        this.inputDirectory = Path.of(config.getInputDirectory());
        this.successDirectory = inputDirectory.resolve("success");
        this.failureDirectory = inputDirectory.resolve("failed");

        try {
            Files.createDirectories(successDirectory);
            Files.createDirectories(failureDirectory);
        } catch (IOException e) {
            log.error("Failed to create output directories", e);
        }
    }

    public void moveToSuccess(String name) {
        moveFile(name, successDirectory);
    }

    public void moveToFailure(String name) {
        moveFile(name, failureDirectory);
    }

    private void moveFile(String name, Path targetDirectory) {
        Path source = inputDirectory.resolve(name);
        if (!Files.exists(source)) {
            log.warn("Source file does not exist: {}", source);
            return;
        }

        try {
            Files.move(source, targetDirectory.resolve(name));
            log.info("Moved {} to {}", name, targetDirectory);
        } catch (IOException e) {
            log.error("Failed to move file {} to {}", name, targetDirectory, e);
        }
    }
}
