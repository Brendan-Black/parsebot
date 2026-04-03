package black.parsebot.model.raw;

import jakarta.mail.Message;


public final class RawMailboxData extends RawData {

    private final Message sourceMessage;

    public RawMailboxData(String name, byte[] content, Message sourceMessage) {
        super(name, content);
        this.sourceMessage = sourceMessage;
    }

    public Message getSourceMessage() {
        return sourceMessage;
    }
}
