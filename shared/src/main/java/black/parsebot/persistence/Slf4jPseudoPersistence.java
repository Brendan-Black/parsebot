package black.parsebot.persistence;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

public final class Slf4jPseudoPersistence<T> implements PseudoPersistence<T> {

  private static final Logger log = LoggerFactory.getLogger(Slf4jPseudoPersistence.class);

  private final Class<T> type;
  private final String marker;
  private final Path logDir;
  private final String logGlob;
  private final Gson gson;

  public Slf4jPseudoPersistence(Class<T> type, String marker, Path logDir, String logGlob) {
    this.type = type;
    this.marker = marker;
    this.logDir = logDir;
    this.logGlob = logGlob;
    this.gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, t, ctx) ->
            new JsonPrimitive(src.toString()))
        .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, t, ctx) ->
            Instant.parse(json.getAsString()))
        .create();
  }

  @Override
  public void append(T item) {
    log.info("{}{}", marker, gson.toJson(item));
  }

  @Override
  public List<T> readLast(int n) throws IOException {
    if (n < 0) throw new IllegalArgumentException("n must be non-negative");
    List<T> all = readAll();
    int from = Math.max(0, all.size() - n);
    List<T> tail = new ArrayList<>(all.subList(from, all.size()));
    java.util.Collections.reverse(tail);
    return tail;
  }

  @Override
  public List<T> readAll() throws IOException {
    List<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir, logGlob)) {
      for (Path p : stream) files.add(p);
    }
    files.sort(Comparator.comparing(p -> p.getFileName().toString()));

    List<T> items = new ArrayList<>();
    for (Path file : files) {
      for (String line : Files.readAllLines(file)) {
        int idx = line.indexOf(marker);
        if (idx < 0) continue;
        String json = line.substring(idx + marker.length());
        try {
          T item = gson.fromJson(json, type);
          if (item != null) items.add(item);
        } catch (Exception e) {
          // Skip malformed lines
        }
      }
    }
    return items;
  }
}
