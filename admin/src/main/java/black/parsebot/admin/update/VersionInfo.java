package black.parsebot.admin.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class VersionInfo {

  private static final String CURRENT = loadVersion();

  public static String current() {
    return CURRENT;
  }

  public static int compare(String a, String b) {
    int[] aa = parts(a);
    int[] bb = parts(b);
    int len = Math.max(aa.length, bb.length);
    for (int i = 0; i < len; i++) {
      int ai = i < aa.length ? aa[i] : 0;
      int bi = i < bb.length ? bb[i] : 0;
      int cmp = Integer.compare(ai, bi);
      if (cmp != 0) return cmp;
    }
    return 0;
  }

  private static int[] parts(String v) {
    String stripped = (v.startsWith("v") || v.startsWith("V")) ? v.substring(1) : v;
    String[] tokens = stripped.split("[.\\-]");
    int[] out = new int[tokens.length];
    for (int i = 0; i < tokens.length; i++) out[i] = leadingInt(tokens[i]);
    return out;
  }

  private static int leadingInt(String s) {
    int end = 0;
    while (end < s.length() && Character.isDigit(s.charAt(end))) end++;
    if (end == 0) return 0;
    try { return Integer.parseInt(s.substring(0, end)); }
    catch (NumberFormatException e) { return 0; }
  }

  private static String loadVersion() {
    try (InputStream in = VersionInfo.class.getResourceAsStream("/version.properties")) {
      if (in == null) return "0.0.0";
      Properties p = new Properties();
      p.load(in);
      return p.getProperty("version", "0.0.0");
    } catch (IOException e) {
      return "0.0.0";
    }
  }

  private VersionInfo() {}
}
