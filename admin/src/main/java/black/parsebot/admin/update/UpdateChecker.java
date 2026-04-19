package black.parsebot.admin.update;

import java.util.List;

public interface UpdateChecker {

  String currentVersion();

  List<ReleaseInfo> listReleases() throws Exception;
}
