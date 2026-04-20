package black.parsebot.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import black.parsebot.config.ConfigKey;
import black.parsebot.config.MailConfig;
import black.parsebot.event.Event;
import black.parsebot.event.EventPublisher;
import black.parsebot.model.raw.RawMailboxData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailboxReaderTest {

  @Test
  void readReturnsEmptyAndPublishesNoEventWhenHostBlank() {
    List<Event> events = new ArrayList<>();
    EventPublisher publisher = events::add;
    MailConfig config = new MailConfig(new Properties());  // host defaults to ""

    MailboxReader reader = new MailboxReader(config, publisher);
    List<RawMailboxData> results = reader.read();

    assertEquals(0, results.size());
    assertTrue(events.isEmpty(), "blank-host skip should not publish an event");
  }

  @Test
  void readPublishesEventWhenConnectionFails() {
    List<Event> events = new ArrayList<>();
    Properties props = new Properties();
    props.setProperty(ConfigKey.MAIL_HOST, "127.0.0.1");
    props.setProperty(ConfigKey.MAIL_PORT, "1");  // nothing listening here
    props.setProperty(ConfigKey.MAIL_USERNAME, "u");
    props.setProperty(ConfigKey.MAIL_PASSWORD, "p");
    props.setProperty(ConfigKey.MAIL_PROTOCOL, "imap");

    MailboxReader reader = new MailboxReader(new MailConfig(props), events::add);
    List<RawMailboxData> results = reader.read();

    assertEquals(0, results.size());
    assertEquals(1, events.size());
  }

  @Test
  void closeIsSafeOnFreshReader() {
    MailboxReader reader = new MailboxReader(new MailConfig(new Properties()), e -> {});
    assertDoesNotThrow(reader::close);
  }
}
