package black.parsebot.model;

public class TransformedData {

    private final String filename;
    private final byte[] content;

    public TransformedData(String filename, byte[] content) {
        this.filename = filename;
        this.content = content;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getContent() {
        return content;
    }
}
