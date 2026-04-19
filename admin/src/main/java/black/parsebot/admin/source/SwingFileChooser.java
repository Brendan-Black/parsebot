package black.parsebot.admin.source;

import java.awt.Frame;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

public final class SwingFileChooser implements FileChooser {

  @Override
  public String choose(Mode mode, String initialPath, String title) {
    AtomicReference<String> result = new AtomicReference<>();
    Runnable task = () -> {
      Frame parent = new Frame();
      parent.setAlwaysOnTop(true);
      parent.setUndecorated(true);
      parent.setSize(1, 1);
      parent.setLocationRelativeTo(null);
      parent.setVisible(true);
      try {
        JFileChooser chooser = new JFileChooser();
        if (title != null && !title.isBlank()) chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(
            mode == Mode.FOLDER ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        applyInitialPath(chooser, initialPath);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
          result.set(chooser.getSelectedFile().getAbsolutePath());
        }
      } finally {
        parent.dispose();
      }
    };
    try {
      if (SwingUtilities.isEventDispatchThread()) task.run();
      else SwingUtilities.invokeAndWait(task);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
    return result.get();
  }

  private static void applyInitialPath(JFileChooser chooser, String initialPath) {
    if (initialPath == null || initialPath.isBlank()) return;
    File f = new File(initialPath);
    if (f.exists()) {
      chooser.setSelectedFile(f);
      return;
    }
    File parent = f.getParentFile();
    if (parent != null && parent.isDirectory()) chooser.setCurrentDirectory(parent);
  }
}
