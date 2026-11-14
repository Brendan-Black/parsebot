package black.parsebot.writer;

import black.parsebot.config.TestConfigs;
import black.parsebot.event.Event;
import black.parsebot.event.EventBus;
import black.parsebot.model.TransformedData;
import black.parsebot.notify.NotificationDispatcher;
import black.parsebot.persistence.PseudoPersistence;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SftpWriterTest {

	private static class NoopStorage implements PseudoPersistence<Event> {
		@Override
		public void append(Event item) {}

		@Override
		public List<Event> readAll() {
			return List.of();
		}

		@Override
		public List<Event> readLast(int n) {
			return List.of();
		}
	}

	@Test
	void unconfiguredHostIsAnErrorNotASkip() {
		// A skipped upload used to let the message be filed as processed, losing its output.
		EventBus bus = new EventBus(new NoopStorage(), new NotificationDispatcher(List.of(), List.of(), new NoopStorage()));
		SftpWriter writer = new SftpWriter(TestConfigs.sftpConfig(new Properties()), bus);
		List<TransformedData> output = List.of(new TransformedData("order.json", "{}".getBytes(StandardCharsets.UTF_8)));

		assertThrows(IOException.class, () -> writer.write(output));
	}
}
