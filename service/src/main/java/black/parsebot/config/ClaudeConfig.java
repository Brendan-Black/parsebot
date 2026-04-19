package black.parsebot.config;

import java.util.Properties;

import black.parsebot.parser.DefaultPdfValidator;

public final class ClaudeConfig {

  private final Properties props;

  ClaudeConfig(Properties props) {
    this.props = props;
  }

  public String getApiKey() {
    return props.getProperty(ConfigKey.CLAUDE_API_KEY, "");
  }

  public long getPdfMaxBytes() {
    return Long.parseLong(
        props.getProperty(ConfigKey.CLAUDE_PDF_MAX_BYTES, String.valueOf(DefaultPdfValidator.DEFAULT_MAX_BYTES)));
  }

  public int getPdfMaxPages() {
    return Integer.parseInt(
        props.getProperty(ConfigKey.CLAUDE_PDF_MAX_PAGES, String.valueOf(DefaultPdfValidator.DEFAULT_MAX_PAGES)));
  }
}
