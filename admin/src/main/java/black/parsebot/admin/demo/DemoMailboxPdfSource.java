package black.parsebot.admin.demo;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import black.parsebot.admin.api.MailboxPdfsApi.EmailAttachment;
import black.parsebot.admin.api.MailboxPdfsApi.EmailMessage;
import black.parsebot.admin.api.MailboxPdfsApi.FetchResponse;
import black.parsebot.admin.api.MailboxPdfsApi.ListResponse;
import black.parsebot.admin.demo.DemoData.DemoMessage;
import black.parsebot.admin.server.JsonHandler.HttpException;
import black.parsebot.admin.source.MailboxPdfSource;

public final class DemoMailboxPdfSource implements MailboxPdfSource {

  private final List<DemoMessage> messages;

  public DemoMailboxPdfSource() {
    this.messages = DemoData.messages();
  }

  @Override
  public ListResponse list() {
    List<EmailMessage> out = new ArrayList<>();
    for (int i = 0; i < messages.size(); i++) {
      DemoMessage m = messages.get(i);
      List<EmailAttachment> atts = List.of(
          new EmailAttachment(0, m.pdfFilename(), m.pdfBytes().length));
      out.add(new EmailMessage(
          i,
          m.subject(),
          m.sender(),
          m.sentAt().toString(),
          atts));
    }
    return new ListResponse(true, out, null);
  }

  @Override
  public FetchResponse fetch(int messageIndex, int attachmentIndex) {
    if (messageIndex < 0 || messageIndex >= messages.size()) {
      throw new HttpException(404, "Message not found");
    }
    if (attachmentIndex != 0) {
      throw new HttpException(404, "Attachment not found");
    }
    DemoMessage m = messages.get(messageIndex);
    return new FetchResponse(m.pdfFilename(), Base64.getEncoder().encodeToString(m.pdfBytes()));
  }
}
