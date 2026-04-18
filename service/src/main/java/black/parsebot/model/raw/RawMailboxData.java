package black.parsebot.model.raw;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class RawMailboxData extends RawData {

    private final Message sourceMessage;

    public RawMailboxData(String name, byte[] content, Message sourceMessage) {
        super(name, content);
        this.sourceMessage = sourceMessage;
    }

    public Message getSourceMessage() {
        return sourceMessage;
    }

    /**
     * Returns the sender's email address, or an empty string if unavailable.
     */
    public String getSenderEmail() {
        try {
            Address[] from = sourceMessage.getFrom();
            if (from != null && from.length > 0 && from[0] instanceof InternetAddress ia) {
                return ia.getAddress().toLowerCase();
            }
        } catch (MessagingException e) {
            // fall through
        }
        return "";
    }

    /**
     * Checks whether the source message contains at least one PDF attachment.
     * Only inspects headers — does not read attachment content.
     */
    public boolean hasPdfAttachments() throws MessagingException, IOException {
        Object content = sourceMessage.getContent();
        if (content instanceof Multipart multipart) {
            return hasPdf(multipart);
        }
        return false;
    }

    private boolean hasPdf(Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getContent() instanceof Multipart nested) {
                if (hasPdf(nested)) return true;
            } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
                    && part.isMimeType("application/pdf")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts all PDF attachments from the source message.
     *
     * @return list of entries where the key is the filename and the value is the PDF bytes
     */
    public List<Map.Entry<String, byte[]>> extractPdfAttachments() throws MessagingException, IOException {
        List<Map.Entry<String, byte[]>> pdfs = new ArrayList<>();
        Object content = sourceMessage.getContent();
        if (content instanceof Multipart multipart) {
            collectPdfs(multipart, pdfs);
        }
        return pdfs;
    }

    private void collectPdfs(Multipart multipart, List<Map.Entry<String, byte[]>> pdfs)
            throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getContent() instanceof Multipart nested) {
                collectPdfs(nested, pdfs);
            } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
                    && part.isMimeType("application/pdf")) {
                String filename = part.getFileName();
                byte[] bytes = part.getInputStream().readAllBytes();
                pdfs.add(new AbstractMap.SimpleImmutableEntry<>(filename, bytes));
            }
        }
    }
}
