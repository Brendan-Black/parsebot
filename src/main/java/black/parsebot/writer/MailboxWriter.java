package black.parsebot.writer;

import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.reader.MailboxReader;

public class MailboxWriter {

	private static final Logger log = LoggerFactory.getLogger(MailboxWriter.class);

	private final Store store;
	private final Folder sourceFolder;

	public MailboxWriter(MailboxReader reader) {
		this.store = reader.getStore();
		this.sourceFolder = reader.getSourceFolder();
	}

	public MailboxWriter(Store store, Folder folder) {
		this.store = store;
		this.sourceFolder = folder;
	}

	public void moveToFolder(Message message, String targetFolderName) {
		try {
			Folder targetFolder = store.getFolder(targetFolderName);
			if (!targetFolder.exists()) {
				targetFolder.create(Folder.HOLDS_MESSAGES);
				log.info("Created IMAP folder: {}", targetFolderName);
			}

			sourceFolder.copyMessages(new Message[] { message }, targetFolder);
			message.setFlag(Flags.Flag.DELETED, true);
			log.info("Moved message '{}' to {}", message.getSubject(), targetFolderName);
		} catch (MessagingException e) {
			log.error("Failed to move message '{}' to {}", message, targetFolderName, e);
		}
	}

	public void close() {
		try {
			if (sourceFolder != null && sourceFolder.isOpen()) {
				sourceFolder.close(true);
			}
			if (store != null && store.isConnected()) {
				store.close();
			}
		} catch (MessagingException e) {
			log.error("Error closing mailbox connection", e);
		}
	}
}
