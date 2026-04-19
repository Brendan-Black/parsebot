package black.parsebot.admin.source;

import black.parsebot.admin.api.MailboxPdfsApi.FetchResponse;
import black.parsebot.admin.api.MailboxPdfsApi.ListResponse;

public interface MailboxPdfSource {

  ListResponse list();

  FetchResponse fetch(int messageIndex, int attachmentIndex);
}
