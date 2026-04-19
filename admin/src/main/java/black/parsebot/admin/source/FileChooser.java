package black.parsebot.admin.source;

public interface FileChooser {

  enum Mode { FILE, FOLDER }

  /** Returns the absolute path of the user's selection, or null if cancelled. */
  String choose(Mode mode, String initialPath, String title);
}
