package black.parsebot.reader;

import black.parsebot.config.AppConfig;
import black.parsebot.model.raw.RawMailboxData;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailboxReader {

    private static final Logger log = LoggerFactory.getLogger(MailboxReader.class);

    private final AppConfig config;
    private Store store;
    private Folder sourceFolder;

    public MailboxReader(AppConfig config) {
        this.config = config;
    }

    public List<RawMailboxData> read() {
        List<RawMailboxData> results = new ArrayList<>();

        if (config.getMailHost().isBlank()) {
            log.debug("Mail host not configured, skipping mailbox read");
            return results;
        }

        Properties mailProps = new Properties();
        String protocol = config.getMailProtocol();
        mailProps.put("mail.store.protocol", protocol);
        mailProps.put("mail." + protocol + ".host", config.getMailHost());
        mailProps.put("mail." + protocol + ".port", String.valueOf(config.getMailPort()));
        mailProps.put("mail." + protocol + ".ssl.enable", "true");

        try {
            Session session = Session.getInstance(mailProps);
            store = session.getStore(protocol);
            store.connect(config.getMailUsername(), config.getMailPassword());

            sourceFolder = store.getFolder(config.getMailFolder());
            sourceFolder.open(Folder.READ_WRITE);

            Message[] messages = sourceFolder.getMessages();
            log.info("Found {} message(s) in {}", messages.length, config.getMailFolder());

            for (Message message : messages) {
                String subject = message.getSubject();
                byte[] content = extractContent(message);
                results.add(new RawMailboxData(subject, content, message));
            }
        } catch (Exception e) {
            log.error("Error reading mailbox", e);
        }

        return results;
    }

    public Store getStore() {
        return store;
    }

    public Folder getSourceFolder() {
        return sourceFolder;
    }

    private byte[] extractContent(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text.getBytes();
        } else if (content instanceof MimeMultipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    sb.append(part.getContent().toString());
                }
            }
            return sb.toString().getBytes();
        }
        return new byte[0];
    }
}
