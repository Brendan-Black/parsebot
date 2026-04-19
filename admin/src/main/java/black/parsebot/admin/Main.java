package black.parsebot.admin;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import black.parsebot.admin.config.ConfigSchema;
import black.parsebot.admin.server.AdminServer;

public class Main {

  public static void main(String[] args) throws Exception {
    List<String> argList = List.of(args);
    String command = argList.stream()
        .filter(a -> !a.startsWith("--"))
        .findFirst()
        .orElse("ui");
    boolean demo = argList.contains("--demo");
    Path exePath = resolveExePath(argList);
    Path logDir = resolveLogDir(argList);
    int port = resolvePort(argList);

    AdminContext context = demo
        ? AdminContext.demo()
        : AdminContext.live(exePath, logDir);

    switch (command) {
      case "uninstall" -> System.exit(context.serviceManager().uninstall());
      case "status" -> System.out.println(context.serviceManager().statusText());
      case "ui", "install", "events" -> launchUi(command, port, context);
      default -> {
        System.err.println("Unknown command: " + command);
        System.err.println("Usage: admin [ui|install|uninstall|status|events] "
            + "[--port <n>] [--exe <path>] [--logs <dir>] [--demo]");
        System.exit(1);
      }
    }
  }

  private static void launchUi(String page, int port, AdminContext context) throws Exception {
    AdminServer server = new AdminServer(context);
    int bound = server.start(port);
    String landing = switch (page) {
      case "install" -> "install";
      case "events" -> "events";
      default -> "launcher";
    };
    String url = "http://127.0.0.1:" + bound + "/#/" + landing;
    System.out.println(context.title() + " UI: " + url);
    openBrowser(url);
    server.awaitShutdown();
    System.out.println("Shutdown complete.");
  }

  private static void openBrowser(String url) {
    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI.create(url));
        return;
      }
    } catch (Exception ignored) {}
    try {
      new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
    } catch (Exception e) {
      System.err.println("Could not open browser automatically. Open manually: " + url);
    }
  }

  private static Path resolveExePath(List<String> args) {
    for (int i = 0; i < args.size() - 1; i++) {
      if ("--exe".equals(args.get(i))) return Path.of(args.get(i + 1)).toAbsolutePath();
    }
    Path adminDir = Path.of(ProcessHandle.current().info().command().orElse(""))
        .toAbsolutePath().getParent();
    if (adminDir != null && Files.isRegularFile(adminDir.resolve(ConfigSchema.SERVICE_EXE))) {
      return adminDir.resolve(ConfigSchema.SERVICE_EXE);
    }
    return Path.of(ConfigSchema.SERVICE_EXE).toAbsolutePath();
  }

  private static Path resolveLogDir(List<String> args) {
    for (int i = 0; i < args.size() - 1; i++) {
      if ("--logs".equals(args.get(i))) return Path.of(args.get(i + 1)).toAbsolutePath();
    }
    return Path.of("logs").toAbsolutePath();
  }

  private static int resolvePort(List<String> args) {
    for (int i = 0; i < args.size() - 1; i++) {
      if ("--port".equals(args.get(i))) {
        try {
          return Integer.parseInt(args.get(i + 1));
        } catch (NumberFormatException e) {
          System.err.println("Invalid --port value: " + args.get(i + 1));
        }
      }
    }
    return 0;
  }
}
