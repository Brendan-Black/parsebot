package black.parsebot.model.raw;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawMailboxDataTest {

  private static final Session SESSION = Session.getInstance(new Properties());

  @Test
  void getSenderEmailReturnsLowercasedFromAddress() throws Exception {
    MimeMessage msg = new MimeMessage(SESSION);
    msg.setFrom(new InternetAddress("Orders@Example.COM"));
    msg.setContent(new MimeMultipart());
    msg.saveChanges();

    RawMailboxData data = new RawMailboxData("subj", new byte[0], msg);
    assertEquals("orders@example.com", data.getSenderEmail());
  }

  @Test
  void getSenderEmailReturnsEmptyStringWhenNoFromHeader() throws Exception {
    MimeMessage msg = new MimeMessage(SESSION);
    msg.setContent(new MimeMultipart());
    msg.saveChanges();

    RawMailboxData data = new RawMailboxData("subj", new byte[0], msg);
    assertEquals("", data.getSenderEmail());
  }

  @Test
  void hasPdfAttachmentsDetectsPdfInMultipart() throws Exception {
    Message msg = messageWithPdf("order.pdf", "%PDF-1.4".getBytes(StandardCharsets.ISO_8859_1));
    RawMailboxData data = new RawMailboxData("subj", new byte[0], msg);
    assertTrue(data.hasPdfAttachments());
  }

  @Test
  void hasPdfAttachmentsReturnsFalseWhenOnlyTextParts() throws Exception {
    MimeMessage msg = new MimeMessage(SESSION);
    MimeMultipart mp = new MimeMultipart();
    MimeBodyPart text = new MimeBodyPart();
    text.setText("no pdfs here");
    mp.addBodyPart(text);
    msg.setContent(mp);
    msg.saveChanges();

    RawMailboxData data = new RawMailboxData("subj", new byte[0], msg);
    assertFalse(data.hasPdfAttachments());
  }

  @Test
  void extractPdfAttachmentsReturnsFilenameAndBytes() throws Exception {
    byte[] pdfBytes = "%PDF-1.4\nhello".getBytes(StandardCharsets.ISO_8859_1);
    Message msg = messageWithPdf("invoice.pdf", pdfBytes);

    RawMailboxData data = new RawMailboxData("subj", new byte[0], msg);
    List<Map.Entry<String, byte[]>> pdfs = data.extractPdfAttachments();

    assertEquals(1, pdfs.size());
    assertEquals("invoice.pdf", pdfs.get(0).getKey());
    assertArrayEquals(pdfBytes, pdfs.get(0).getValue());
  }

  @Test
  void extractPdfAttachmentsIgnoresNonPdfAttachments() throws Exception {
    MimeMessage msg = new MimeMessage(SESSION);
    MimeMultipart mp = new MimeMultipart();

    MimeBodyPart txtAttachment = new MimeBodyPart();
    txtAttachment.setContent("notes", "text/plain");
    txtAttachment.setFileName("notes.txt");
    txtAttachment.setDisposition(Part.ATTACHMENT);
    mp.addBodyPart(txtAttachment);

    MimeBodyPart pdfAttachment = new MimeBodyPart();
    pdfAttachment.setDataHandler(new DataHandler(
        new ByteArrayDataSource("%PDF-1.4".getBytes(StandardCharsets.ISO_8859_1), "application/pdf")));
    pdfAttachment.setFileName("order.pdf");
    pdfAttachment.setDisposition(Part.ATTACHMENT);
    mp.addBodyPart(pdfAttachment);

    msg.setContent(mp);
    msg.saveChanges();

    RawMailboxData data = new RawMailboxData("subj", new byte[0], msg);
    List<Map.Entry<String, byte[]>> pdfs = data.extractPdfAttachments();

    assertEquals(1, pdfs.size());
    assertEquals("order.pdf", pdfs.get(0).getKey());
  }

  @Test
  void extractPdfAttachmentsRecursesIntoNestedMultipart() throws Exception {
    MimeMessage msg = new MimeMessage(SESSION);
    MimeMultipart outer = new MimeMultipart();

    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setText("body");
    outer.addBodyPart(textPart);

    MimeMultipart inner = new MimeMultipart();
    MimeBodyPart pdfAttachment = new MimeBodyPart();
    pdfAttachment.setDataHandler(new DataHandler(
        new ByteArrayDataSource("%PDF-1.4".getBytes(StandardCharsets.ISO_8859_1), "application/pdf")));
    pdfAttachment.setFileName("nested.pdf");
    pdfAttachment.setDisposition(Part.ATTACHMENT);
    inner.addBodyPart(pdfAttachment);
    MimeBodyPart innerWrapper = new MimeBodyPart();
    innerWrapper.setContent(inner);
    outer.addBodyPart(innerWrapper);

    msg.setContent(outer);
    msg.saveChanges();

    RawMailboxData data = new RawMailboxData("subj", new byte[0], msg);
    List<Map.Entry<String, byte[]>> pdfs = data.extractPdfAttachments();

    assertEquals(1, pdfs.size());
    assertEquals("nested.pdf", pdfs.get(0).getKey());
  }

  private static Message messageWithPdf(String filename, byte[] pdfBytes) throws Exception {
    MimeMessage msg = new MimeMessage(SESSION);
    MimeMultipart mp = new MimeMultipart();

    MimeBodyPart body = new MimeBodyPart();
    body.setText("here is the order");
    mp.addBodyPart(body);

    MimeBodyPart pdf = new MimeBodyPart();
    pdf.setDataHandler(new DataHandler(new ByteArrayDataSource(pdfBytes, "application/pdf")));
    pdf.setFileName(filename);
    pdf.setDisposition(Part.ATTACHMENT);
    mp.addBodyPart(pdf);

    msg.setContent(mp);
    msg.saveChanges();
    return msg;
  }
}
