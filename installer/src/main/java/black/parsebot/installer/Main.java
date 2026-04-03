package black.parsebot.installer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final String SERVICE_NAME = "ParseBot";
    private static final String DISPLAY_NAME = "ParseBot Service";
    private static final String DESCRIPTION = "ParseBot email parsing service";
    private static final String SERVICE_EXE = "service.exe";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: installer <install|uninstall|status> [--exe <path>]");
            System.exit(1);
        }

        String command = args[0];
        Path exePath = resolveExePath(args);

        switch (command) {
            case "install" -> install(exePath);
            case "uninstall" -> uninstall();
            case "status" -> status();
            default -> {
                System.err.println("Unknown command: " + command);
                System.exit(1);
            }
        }
    }

    private static Path resolveExePath(String[] args) {
        for (int i = 1; i < args.length - 1; i++) {
            if ("--exe".equals(args[i])) {
                return Path.of(args[i + 1]).toAbsolutePath();
            }
        }
        // Default: assume service.exe is next to the installer
        Path jarDir = Path.of("").toAbsolutePath();
        return jarDir.resolve("service").resolve(SERVICE_EXE);
    }

    private static void install(Path exePath) {
        if (!Files.isRegularFile(exePath)) {
            System.err.println("Service executable not found: " + exePath);
            System.exit(1);
        }

        String binPath = exePath.toAbsolutePath().toString();

        System.out.println("Installing " + SERVICE_NAME + "...");
        System.out.println("Executable: " + binPath);

        int rc = sc("create", SERVICE_NAME,
                "binPath=", binPath,
                "DisplayName=", DISPLAY_NAME,
                "start=", "auto");
        if (rc != 0) {
            System.err.println("Failed to create service (exit code " + rc + ").");
            System.exit(rc);
        }

        rc = sc("description", SERVICE_NAME, DESCRIPTION);
        if (rc != 0) {
            System.err.println("Warning: could not set service description (exit code " + rc + ").");
        }

        System.out.println(SERVICE_NAME + " installed successfully.");
        System.out.println("Start it with: sc start " + SERVICE_NAME);
    }

    private static void uninstall() {
        System.out.println("Stopping " + SERVICE_NAME + "...");
        sc("stop", SERVICE_NAME);

        System.out.println("Removing " + SERVICE_NAME + "...");
        int rc = sc("delete", SERVICE_NAME);
        if (rc != 0) {
            System.err.println("Failed to remove service (exit code " + rc + ").");
            System.exit(rc);
        }
        System.out.println(SERVICE_NAME + " uninstalled.");
    }

    private static void status() {
        sc("query", SERVICE_NAME);
    }

    private static int sc(String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "sc.exe";
        System.arraycopy(args, 0, command, 1, args.length);
        try {
            Process process = new ProcessBuilder(command)
                    .inheritIO()
                    .start();
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to run sc.exe: " + e.getMessage());
            return 1;
        }
    }
}
