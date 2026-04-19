package black.parsebot.admin.update;

public interface UpdateApplier {

  ApplyResult apply(ReleaseInfo release) throws Exception;

  record ApplyResult(boolean success, String message) {}
}
