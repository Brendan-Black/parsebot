package black.parsebot.admin.update;

public record ReleaseInfo(
    String tag,
    String name,
    String publishedAt,
    String zipAssetUrl,
    long zipAssetSize,
    String notes,
    boolean isCurrent,
    boolean isNewer,
    boolean prerelease
) {}
