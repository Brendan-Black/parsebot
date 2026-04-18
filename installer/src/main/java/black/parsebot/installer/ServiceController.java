package black.parsebot.installer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static black.parsebot.installer.InstallerConfig.*;

final class ServiceController {

    private final InstallerUi ui;

    ServiceController(InstallerUi ui) {
        this.ui = ui;
    }

    void install(Path exePath, boolean dryRun) {
        if (!dryRun && !Files.isRegularFile(exePath)) {
            System.err.println("Service executable not found: " + exePath);
            System.exit(1);
        }

        Map<String, String> existing = loadExistingConfig();
        Map<String, String> values = ui.showConfigDialog(existing);
        if (values == null) {
            System.out.println("Installation cancelled.");
            System.exit(0);
        }

        StringBuilder binPath = new StringBuilder();
        binPath.append('"').append(exePath.toAbsolutePath()).append('"');
        for (var entry : values.entrySet()) {
            String val = entry.getValue();
            if (!val.isEmpty()) {
                binPath.append(" \"-Dparsebot.").append(entry.getKey()).append('=').append(val).append('"');
            }
        }

        System.out.println("binPath: " + binPath);

        if (dryRun) {
            System.out.println("[dry-run] Would run: sc create " + SERVICE_NAME + " binPath= " + binPath);
            return;
        }

        System.out.println("Installing " + SERVICE_NAME + "...");

        int rc = sc("create", SERVICE_NAME,
                "binPath=", binPath.toString(),
                "DisplayName=", DISPLAY_NAME,
                "start=", "auto");
        if (rc != 0) {
            System.err.println("Failed to create service (exit code " + rc + ").");
            System.exit(rc);
        }

        sc("description", SERVICE_NAME, DESCRIPTION);
        sc("failure", SERVICE_NAME, "reset=", "86400", "actions=", "restart/5000/restart/10000/restart/30000");
        System.out.println(SERVICE_NAME + " installed successfully. Starting...");

        int startRc = sc("start", SERVICE_NAME);
        if (startRc != 0) {
            System.err.println("Service installed but failed to start (exit code " + startRc + ").");
            System.err.println("You can start it manually: sc start " + SERVICE_NAME);
        } else {
            System.out.println(SERVICE_NAME + " is running.");
        }
    }

    void uninstall() {
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

    void status() {
        sc("query", SERVICE_NAME);
    }

    Map<String, String> loadExistingConfig() {
        try {
            Process proc = new ProcessBuilder("sc.exe", "qc", SERVICE_NAME)
                    .redirectErrorStream(true)
                    .start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            int rc = proc.waitFor();
            if (rc != 0) {
                return Collections.emptyMap();
            }

            String raw = output.toString();
            Map<String, String> config = new LinkedHashMap<>();
            Pattern pattern = Pattern.compile("-Dparsebot\\.([^=]+)=([^\"]*)");
            Matcher matcher = pattern.matcher(raw);
            while (matcher.find()) {
                config.put(matcher.group(1), matcher.group(2).trim());
            }
            return config;
        } catch (IOException | InterruptedException e) {
            return Collections.emptyMap();
        }
    }

    static Path resolveExePath(String[] args) {
        for (int i = 1; i < args.length - 1; i++) {
            if ("--exe".equals(args[i])) {
                return Path.of(args[i + 1]).toAbsolutePath();
            }
        }
        Path installerDir = Path.of(ProcessHandle.current().info().command().orElse(""))
                .toAbsolutePath().getParent();
        if (installerDir != null && Files.isRegularFile(installerDir.resolve(SERVICE_EXE))) {
            return installerDir.resolve(SERVICE_EXE);
        }
        return Path.of("").toAbsolutePath().resolve(SERVICE_EXE);
    }

    private static int sc(String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "sc.exe";
        System.arraycopy(args, 0, command, 1, args.length);
        try {
            return new ProcessBuilder(command).inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to run sc.exe: " + e.getMessage());
            return 1;
        }
    }
}
