package black.parsebot.admin.source;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import black.parsebot.admin.api.MailboxPdfsApi.EmailAttachment;
import black.parsebot.admin.api.MailboxPdfsApi.EmailMessage;
import black.parsebot.admin.api.MailboxPdfsApi.FetchResponse;
import black.parsebot.admin.api.MailboxPdfsApi.ListResponse;
import black.parsebot.admin.server.JsonHandler.HttpException;
import black.parsebot.admin.service.ServiceManager;
import black.parsebot.config.MailConfig;
import black.parsebot.event.EventPublisher;
import black.parsebot.model.raw.RawMailboxData;
import black.parsebot.reader.MailboxReader;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;

public final class ImapMailboxPdfSource implements MailboxPdfSource {

  private static final EventPublisher NOOP = event -> {};

  private final ServiceManager serviceManager;

  public ImapMailboxPdfSource(ServiceManager serviceManager) {
    this.serviceManager = serviceManager;
  }

  @Override
  public ListResponse list() {
    MailConfig mailConfig = buildMailConfig();
    if (mailConfig.getHost().isBlank()) {
      return new ListResponse(false, Collections.emptyList(), null);
    }

    MailboxReader reader = new MailboxReader(mailConfig, NOOP);
    try {
      reader.ensureConnected();
      Message[] messages = reader.getSourceFolder().getMessages();
      List<EmailMessage> out = new ArrayList<>();
      for (int i = 0; i < messages.length; i++) {
        Message msg = messages[i];
        RawMailboxData data = new RawMailboxData(msg.getSubject(), new byte[0], msg);
        if (!data.hasPdfAttachments()) continue;
        List<EmailAttachment> atts = new ArrayList<>();
        List<Map.Entry<String, byte[]>> pdfs = data.extractPdfAttachments();
        for (int a = 0; a < pdfs.size(); a++) {
          Map.Entry<String, byte[]> pdf = pdfs.get(a);
          atts.add(new EmailAttachment(a, pdf.getKey(), pdf.getValue().length));
        }
        out.add(new EmailMessage(
            i,
            nullToEmpty(msg.getSubject()),
            formatFrom(msg.getFrom()),
            formatDate(msg),
            atts));
      }
      return new ListResponse(true, out, null);
    } catch (Exception e) {
      return new ListResponse(true, Collections.emptyList(),
          e.getClass().getSimpleName() + ": " + e.getMessage());
    } finally {
      reader.close();
    }
  }

  @Override
  public FetchResponse fetch(int messageIndex, int attachmentIndex) {
    MailConfig mailConfig = buildMailConfig();
    if (mailConfig.getHost().isBlank()) {
      throw new HttpException(400, "Mail is not configured");
    }

    MailboxReader reader = new MailboxReader(mailConfig, NOOP);
    try {
      reader.ensureConnected();
      Message[] messages = reader.getSourceFolder().getMessages();
      if (messageIndex >= messages.length) {
        throw new HttpException(404, "Message not found");
      }
      RawMailboxData data = new RawMailboxData(
          messages[messageIndex].getSubject(), new byte[0], messages[messageIndex]);
      List<Map.Entry<String, byte[]>> pdfs = data.extractPdfAttachments();
      if (attachmentIndex >= pdfs.size()) {
        throw new HttpException(404, "Attachment not found");
      }
      Map.Entry<String, byte[]> pdf = pdfs.get(attachmentIndex);
      return new FetchResponse(pdf.getKey(), Base64.getEncoder().encodeToString(pdf.getValue()));
    } catch (HttpException e) {
      throw e;
    } catch (Exception e) {
      throw new HttpException(500, e.getClass().getSimpleName() + ": " + e.getMessage());
    } finally {
      reader.close();
    }
  }

  private MailConfig buildMailConfig() {
    Properties props = new Properties();
    props.putAll(serviceManager.currentConfig());
    return new MailConfig(props);
  }

  private static String formatFrom(Address[] from) {
    if (from == null || from.length == 0) return "";
    Address a = from[0];
    if (a instanceof InternetAddress ia) {
      String personal = ia.getPersonal();
      return personal != null && !personal.isBlank()
          ? personal + " <" + ia.getAddress() + ">"
          : ia.getAddress();
    }
    return a.toString();
  }

  private static String formatDate(Message msg) {
    try {
      java.util.Date d = msg.getSentDate();
      if (d == null) d = msg.getReceivedDate();
      return d != null ? d.toInstant().toString() : "";
    } catch (Exception e) {
      return "";
    }
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
