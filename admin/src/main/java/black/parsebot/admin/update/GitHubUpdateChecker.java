package black.parsebot.admin.update;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public final class GitHubUpdateChecker implements UpdateChecker {

  private static final Gson GSON = new Gson();

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  @Override
  public String currentVersion() {
    return VersionInfo.current();
  }

  @Override
  public List<ReleaseInfo> listReleases() throws Exception {
    HttpRequest req = HttpRequest.newBuilder(URI.create(GitHubConfig.apiBase() + "/releases?per_page=50"))
        .header("Accept", "application/vnd.github+json")
        .header("Authorization", "Bearer " + GitHubConfig.pat())
        .header("X-GitHub-Api-Version", "2022-11-28")
        .timeout(Duration.ofSeconds(30))
        .GET()
        .build();
    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() / 100 != 2) {
      throw new RuntimeException("GitHub API " + res.statusCode() + ": " + truncate(res.body()));
    }
    GhRelease[] releases = GSON.fromJson(res.body(), GhRelease[].class);
    String current = currentVersion();
    List<ReleaseInfo> out = new ArrayList<>();
    for (GhRelease r : releases) {
      if (r == null || r.draft) continue;
      String zipUrl = null;
      long zipSize = 0;
      if (r.assets != null) {
        for (GhAsset a : r.assets) {
          if (a == null || a.name == null) continue;
          if (a.name.toLowerCase().endsWith(".zip")) {
            zipUrl = a.url;
            zipSize = a.size;
            break;
          }
        }
      }
      if (zipUrl == null) continue;
      int cmp = VersionInfo.compare(r.tagName, current);
      out.add(new ReleaseInfo(
          r.tagName,
          r.name != null && !r.name.isBlank() ? r.name : r.tagName,
          r.publishedAt,
          zipUrl,
          zipSize,
          r.body != null ? r.body : "",
          cmp == 0,
          cmp > 0,
          r.prerelease
      ));
    }
    return out;
  }

  private static String truncate(String body) {
    if (body == null) return "";
    return body.length() <= 300 ? body : body.substring(0, 300) + "…";
  }

  private static final class GhRelease {
    @SerializedName("tag_name") String tagName;
    String name;
    @SerializedName("published_at") String publishedAt;
    String body;
    boolean draft;
    boolean prerelease;
    GhAsset[] assets;
  }

  private static final class GhAsset {
    String name;
    String url;
    long size;
  }
}
