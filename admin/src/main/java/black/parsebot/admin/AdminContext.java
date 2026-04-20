package black.parsebot.admin;

import java.net.http.HttpClient;
import java.nio.file.Path;

import black.parsebot.admin.config.ConfigSchema;
import black.parsebot.admin.demo.DemoEventsSource;
import black.parsebot.admin.demo.DemoMailboxPdfSource;
import black.parsebot.admin.demo.DemoReferenceDataSource;
import black.parsebot.admin.demo.DemoServiceManager;
import black.parsebot.admin.parser.ClaudeApiOrderParser;
import black.parsebot.admin.parser.ClaudeCliOrderParser;
import black.parsebot.admin.parser.OrderParser;
import black.parsebot.admin.service.ServiceManager;
import black.parsebot.admin.service.WindowsServiceManager;
import black.parsebot.admin.source.EventsSource;
import black.parsebot.admin.source.FileChooser;
import black.parsebot.admin.source.FileReferenceDataSource;
import black.parsebot.admin.source.ImapMailboxPdfSource;
import black.parsebot.admin.source.MailboxPdfSource;
import black.parsebot.admin.source.ReferenceDataSource;
import black.parsebot.admin.source.Slf4jPseudoEventsSource;
import black.parsebot.admin.source.SwingFileChooser;
import black.parsebot.admin.update.DemoUpdateApplier;
import black.parsebot.admin.update.DemoUpdateChecker;
import black.parsebot.admin.update.GitHubUpdateChecker;
import black.parsebot.admin.update.UpdateApplier;
import black.parsebot.admin.update.UpdateChecker;
import black.parsebot.admin.update.WindowsUpdateApplier;

public record AdminContext(
    boolean demoMode,
    ServiceManager serviceManager,
    ReferenceDataSource referenceData,
    EventsSource events,
    MailboxPdfSource mailboxPdfs,
    OrderParser orderParser,
    FileChooser fileChooser,
    UpdateChecker updateChecker,
    UpdateApplier updateApplier
) {

  public String title() {
    return demoMode ? ConfigSchema.DISPLAY_NAME + " (demo mode)" : ConfigSchema.DISPLAY_NAME;
  }

  public static AdminContext live(Path exePath, Path logDir) {
    ServiceManager sm = new WindowsServiceManager(exePath);
    Path installDir = exePath.getParent();
    return new AdminContext(
        false,
        sm,
        new FileReferenceDataSource(sm),
        new Slf4jPseudoEventsSource(logDir),
        new ImapMailboxPdfSource(sm),
        new ClaudeApiOrderParser(HttpClient.newHttpClient()),
        new SwingFileChooser(),
        new GitHubUpdateChecker(),
        new WindowsUpdateApplier(installDir, ConfigSchema.SERVICE_NAME, ConfigSchema.ADMIN_EXE));
  }

  public static AdminContext demo() {
    ServiceManager sm = new DemoServiceManager();
    return new AdminContext(
        true,
        sm,
        new DemoReferenceDataSource(),
        new DemoEventsSource(),
        new DemoMailboxPdfSource(),
        new ClaudeCliOrderParser(),
        new SwingFileChooser(),
        new DemoUpdateChecker(),
        new DemoUpdateApplier());
  }
}
