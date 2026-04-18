package black.parsebot.installer;

import java.util.Map;

public interface InstallerUi {

    String showLauncherDialog();

    Map<String, String> showConfigDialog(Map<String, String> existing);
}
