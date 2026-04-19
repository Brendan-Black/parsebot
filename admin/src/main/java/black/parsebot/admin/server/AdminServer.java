package black.parsebot.admin.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import black.parsebot.admin.AdminContext;
import black.parsebot.admin.api.ConfigApi;
import black.parsebot.admin.api.EventsApi;
import black.parsebot.admin.api.FileChooserApi;
import black.parsebot.admin.api.HeartbeatApi;
import black.parsebot.admin.api.MailboxPdfsApi;
import black.parsebot.admin.api.ModeApi;
import black.parsebot.admin.api.ReferenceDataApi;
import black.parsebot.admin.api.ServiceApi;
import black.parsebot.admin.api.ShutdownApi;
import black.parsebot.admin.api.TestParseApi;

public final class AdminServer {

  private static final long HEARTBEAT_TIMEOUT_MS = 10_000;
  private static final long WATCHDOG_INTERVAL_MS = 2_000;
  private static final long INITIAL_GRACE_MS = 60_000;

  private final AdminContext context;
  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  private HttpServer httpServer;
  private ScheduledExecutorService watchdog;
  private volatile long lastHeartbeatMs = 0;
  private volatile boolean heartbeatSeen = false;

  public AdminServer(AdminContext context) {
    this.context = context;
  }

  public int start(int requestedPort) throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", requestedPort), 0);
    httpServer.createContext("/api/schema", new ConfigApi.SchemaHandler());
    httpServer.createContext("/api/config", new ConfigApi.ConfigHandler(context.serviceManager()));
    httpServer.createContext("/api/service/install", new ServiceApi.InstallHandler(context.serviceManager()));
    httpServer.createContext("/api/service/uninstall", new ServiceApi.UninstallHandler(context.serviceManager()));
    httpServer.createContext("/api/service/status", new ServiceApi.StatusHandler(context.serviceManager()));
    httpServer.createContext("/api/events", new EventsApi.EventsHandler(context.events()));
    httpServer.createContext("/api/reference-data", new ReferenceDataApi.Handler(context.referenceData()));
    httpServer.createContext("/api/email-pdfs", new MailboxPdfsApi.ListHandler(context.mailboxPdfs()));
    httpServer.createContext("/api/email-pdf", new MailboxPdfsApi.FetchHandler(context.mailboxPdfs()));
    httpServer.createContext("/api/test-parse", new TestParseApi.Handler(context.orderParser(), !context.demoMode()));
    httpServer.createContext("/api/heartbeat", new HeartbeatApi.Handler(this));
    httpServer.createContext("/api/shutdown", new ShutdownApi.Handler(this));
    httpServer.createContext("/api/mode", new ModeApi.Handler(context));
    httpServer.createContext("/api/file-chooser", new FileChooserApi.Handler(context.fileChooser()));
    httpServer.createContext("/", new StaticHandler());
    httpServer.setExecutor(null);
    httpServer.start();
    startWatchdog();
    return httpServer.getAddress().getPort();
  }

  public void recordHeartbeat() {
    lastHeartbeatMs = System.currentTimeMillis();
    heartbeatSeen = true;
  }

  public void shutdown() {
    if (watchdog != null) watchdog.shutdownNow();
    if (httpServer != null) httpServer.stop(1);
    shutdownLatch.countDown();
  }

  public void awaitShutdown() throws InterruptedException {
    shutdownLatch.await();
  }

  private void startWatchdog() {
    watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "admin-watchdog");
      t.setDaemon(true);
      return t;
    });
    long startMs = System.currentTimeMillis();
    watchdog.scheduleAtFixedRate(() -> watchdogCheck(startMs),
        WATCHDOG_INTERVAL_MS, WATCHDOG_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  private void watchdogCheck(long startMs) {
    long now = System.currentTimeMillis();
    if (!heartbeatSeen) {
      if (now - startMs > INITIAL_GRACE_MS) {
        System.err.println("No browser ever connected — shutting down.");
        shutdown();
      }
      return;
    }
    long elapsed = now - lastHeartbeatMs;
    if (elapsed > HEARTBEAT_TIMEOUT_MS) {
      System.err.println("No heartbeat for " + elapsed + "ms — shutting down.");
      shutdown();
    }
  }
}
