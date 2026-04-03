package black.parsebot.writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.config.AppConfig.FileSystemConfig;

public class FileSystemWriter {

	private static final Logger log = LoggerFactory.getLogger(FileSystemWriter.class);

	private final Path center;

	public FileSystemWriter(FileSystemConfig config) {
		this.center = Path.of(config.getCenter());
	}

	public void moveFile(String name, Path targetDirectory) {
		Path source = center.resolve(name);
		moveFile(source, targetDirectory);
	}

	public void moveFile(Path source, Path targetDirectory) {

		if (!Files.exists(source)) {
			log.warn("Source file does not exist: {}", source);
			return;
		}
		try {
			Files.move(source, targetDirectory.resolve(source));
			log.info("Moved {} to {}", source, targetDirectory);
		} catch (IOException e) {
			log.error("Failed to move file {} to {}", source, targetDirectory, e);
		}
	}
}
