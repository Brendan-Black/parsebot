package black.parsebot.installer;

import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<String> argList = List.of(args);
        boolean dryRun = argList.contains("--dry-run");
        Path exePath = ServiceController.resolveExePath(args);

        InstallerUi ui = new PowerShellInstallerUi();

        String command = argList.stream()
                .filter(a -> !a.startsWith("--"))
                .findFirst()
                .orElse(null);

        if (command == null) {
            command = ui.showLauncherDialog();
            if (command == null) {
                System.out.println("Cancelled.");
                System.exit(0);
            }
        }

        ServiceController controller = new ServiceController(ui);
        switch (command) {
            case "install" -> controller.install(exePath, dryRun);
            case "uninstall" -> controller.uninstall();
            case "status" -> controller.status();
            default -> {
                System.err.println("Unknown command: " + command);
                System.exit(1);
            }
        }
    }
}
