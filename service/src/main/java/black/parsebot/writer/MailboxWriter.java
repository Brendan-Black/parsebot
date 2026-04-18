package black.parsebot.writer;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import black.parsebot.event.Event;
import black.parsebot.event.EventBus;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import black.parsebot.reader.MailboxReader;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

public class MailboxWriter {

  private static final Logger log = LoggerFactory.getLogger(MailboxWriter.class);

  private final MailboxReader reader;
  private final EventBus eventBus;

  public MailboxWriter(MailboxReader reader, EventBus eventBus) {
    this.reader = reader;
    this.eventBus = eventBus;
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
      String subject;
      try { subject = message.getSubject(); } catch (MessagingException ignored) { subject = "<unknown>"; }
      eventBus.publish(Event.create(
          EventType.MAILBOX_WRITE_FAILED,
          EventSeverity.AUDIT,
          "Failed to move message to " + targetFolderName,
          Map.of(
              "subject", subject != null ? subject : "<null>",
              "targetFolder", targetFolderName,
              "error", String.valueOf(e.getMessage())
          )));
    }
  }
}
