package black.parsebot.persistence;

import java.time.Instant;
import java.util.UUID;

public class ProcessingRecord {

  private UUID id;
  private String emailAddress;
  private String pdfHash;
  private boolean success;
  private Instant timestamp;

  public ProcessingRecord() {}

  public ProcessingRecord(String emailAddress, String pdfHash, boolean success, Instant timestamp) {
    this.id = UUID.randomUUID();
    this.emailAddress = emailAddress;
    this.pdfHash = pdfHash;
    this.success = success;
    this.timestamp = timestamp;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getEmailAddress() { return emailAddress; }
  public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

  public String getPdfHash() { return pdfHash; }
  public void setPdfHash(String pdfHash) { this.pdfHash = pdfHash; }

  public boolean isSuccess() { return success; }
  public void setSuccess(boolean success) { this.success = success; }

  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
