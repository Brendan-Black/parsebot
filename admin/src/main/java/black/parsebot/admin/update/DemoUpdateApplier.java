package black.parsebot.admin.update;

public final class DemoUpdateApplier implements UpdateApplier {

  @Override
  public ApplyResult apply(ReleaseInfo release) {
    return new ApplyResult(false, "Updates are disabled in demo mode.");
  }
}
