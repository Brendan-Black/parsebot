package black.parsebot.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * JSON-file backed store. The per-email cap is an implementation detail of this
 * backend to keep the file bounded; it is NOT part of the repository contract.
 * A future SQL-backed implementation would retain all rows and rely on
 * {@code LIMIT} at query time instead.
 */
public final class JsonFileProcessingHistoryRepository implements ProcessingHistoryRepository {

  private final Path file;
  private final int perEmailCap;
  private final Gson gson;

  public JsonFileProcessingHistoryRepository(Path file, int perEmailCap) {
    if (perEmailCap < 1) throw new IllegalArgumentException("perEmailCap must be >= 1");
    this.file = file;
    this.perEmailCap = perEmailCap;
    this.gson = new GsonBuilder()
        .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, t, ctx) ->
            new JsonPrimitive(src.toString()))
        .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, t, ctx) ->
            Instant.parse(json.getAsString()))
        .setPrettyPrinting()
        .create();
  }

  @Override
  public synchronized ProcessingRecord save(ProcessingRecord entity) {
    if (entity.getId() == null) entity.setId(UUID.randomUUID());
    Map<String, List<ProcessingRecord>> store = loadMap();
    String key = entity.getEmailAddress();
    List<ProcessingRecord> entries = store.computeIfAbsent(key, k -> new ArrayList<>());
    int existing = indexOfId(entries, entity.getId());
    if (existing >= 0) {
      entries.set(existing, entity);
    } else {
      entries.add(0, entity);
      if (entries.size() > perEmailCap) {
        entries.subList(perEmailCap, entries.size()).clear();
      }
    }
    writeMap(store);
    return entity;
  }

  @Override
  public synchronized Optional<ProcessingRecord> findById(UUID id) {
    if (id == null) return Optional.empty();
    Map<String, List<ProcessingRecord>> store = loadMap();
    for (List<ProcessingRecord> entries : store.values()) {
      for (ProcessingRecord r : entries) {
        if (id.equals(r.getId())) return Optional.of(r);
      }
    }
    return Optional.empty();
  }

  @Override
  public synchronized void delete(ProcessingRecord entity) {
    if (entity == null || entity.getId() == null) return;
    Map<String, List<ProcessingRecord>> store = loadMap();
    List<ProcessingRecord> entries = store.get(entity.getEmailAddress());
    if (entries == null) return;
    boolean removed = entries.removeIf(r -> entity.getId().equals(r.getId()));
    if (removed) writeMap(store);
  }

  @Override
  public synchronized List<ProcessingRecord> findRecentByEmailAddress(String emailAddress, int limit) {
    if (limit < 0) throw new IllegalArgumentException("limit must be non-negative");
    if (limit == 0) return List.of();
    Map<String, List<ProcessingRecord>> store = loadMap();
    List<ProcessingRecord> entries = store.get(emailAddress);
    if (entries == null || entries.isEmpty()) return List.of();
    return new ArrayList<>(entries.subList(0, Math.min(limit, entries.size())));
  }

  private static int indexOfId(List<ProcessingRecord> entries, UUID id) {
    for (int i = 0; i < entries.size(); i++) {
      if (id.equals(entries.get(i).getId())) return i;
    }
    return -1;
  }

  private Map<String, List<ProcessingRecord>> loadMap() {
    if (!Files.exists(file)) return new LinkedHashMap<>();
    try {
      String json = Files.readString(file);
      if (json.isBlank()) return new LinkedHashMap<>();
      Type listType = TypeToken.getParameterized(List.class, ProcessingRecord.class).getType();
      Type mapType = TypeToken.getParameterized(Map.class, String.class, listType).getType();
      Map<String, List<ProcessingRecord>> parsed = gson.fromJson(json, mapType);
      return parsed != null ? parsed : new LinkedHashMap<>();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeMap(Map<String, List<ProcessingRecord>> store) {
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) Files.createDirectories(parent);
      Path tmp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
      Files.writeString(tmp, gson.toJson(store));
      try {
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
