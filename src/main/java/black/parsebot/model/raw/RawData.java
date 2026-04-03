package black.parsebot.model.raw;


public sealed abstract class RawData permits RawFileData, RawMailboxData {

    private final String name;
    private final byte[] content;

    protected RawData(String name, byte[] content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }
}
