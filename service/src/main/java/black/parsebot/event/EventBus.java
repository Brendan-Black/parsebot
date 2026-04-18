package black.parsebot.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.Instant;

import black.parsebot.notify.NotificationDispatcher;

public class EventBus {

	private static final Logger log = LoggerFactory.getLogger(EventBus.class);
	private static final String EVENT_PREFIX = "EVENT: ";

	private final NotificationDispatcher dispatcher;
	private final Gson gson;

	public EventBus(NotificationDispatcher dispatcher) {
		this.dispatcher = dispatcher;
		this.gson = new GsonBuilder()
				.registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, type, ctx) ->
						new JsonPrimitive(src.toString()))
				.create();
	}

	public void publish(Event event) {
		log.info("{}{}", EVENT_PREFIX, gson.toJson(event));

		try {
			dispatcher.dispatch(event);
		} catch (Exception e) {
			log.error("Failed to dispatch notification for event {}", event.id(), e);
		}
	}
}
