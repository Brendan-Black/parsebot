package black.parsebot.model;

import jakarta.mail.Message;


public class RawData {

    public enum Source {
        FILE_SYSTEM,
        MAILBOX
    }

    private final String name;
    private final byte[] content;
    private final Source source;
    private final Message sourceMessage;

    public RawData(String name, byte[] content, Source source,  Message sourceMessage) {
        this.name = name;
        this.content = content;
        this.source = source;
        this.sourceMessage = sourceMessage;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public Source getSource() {
        return source;
    }

    public Message getSourceMessage() {
        return sourceMessage;
    }
}
