package black.parsebot.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.reader.MailboxReader;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

public class MailboxWriter {

	private static final Logger log = LoggerFactory.getLogger(MailboxWriter.class);

	private final MailboxReader reader;

	public MailboxWriter(MailboxReader reader) {
		this.reader = reader;
	}

	public void moveToFolder(Message message, String targetFolderName) {
		try {
			Folder targetFolder = reader.getStore().getFolder(targetFolderName);
			if (!targetFolder.exists()) {
				targetFolder.create(Folder.HOLDS_MESSAGES);
				log.info("Created IMAP folder: {}", targetFolderName);
			}

			reader.getSourceFolder().copyMessages(new Message[] { message }, targetFolder);
			message.setFlag(Flags.Flag.DELETED, true);
			log.info("Moved message '{}' to {}", message.getSubject(), targetFolderName);
		} catch (MessagingException e) {
			log.error("Failed to move message '{}' to {}", message, targetFolderName, e);
		}
	}
}
