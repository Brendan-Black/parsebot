package black.parsebot.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFileProcessingHistoryRepositoryTest {

  private static JsonFileProcessingHistoryRepository repo(Path file, int cap) {
    return new JsonFileProcessingHistoryRepository(file, cap);
  }

  private static ProcessingRecord rec(String email, String hash, boolean success) {
    return new ProcessingRecord(email, hash, success, Instant.now());
  }

  @Test
  void saveAssignsIdIfAbsent(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    ProcessingRecord pr = rec("a@x.com", "h1", true);
    pr.setId(null);

    ProcessingRecord saved = r.save(pr);

    assertTrue(saved.getId() != null);
    assertTrue(r.findById(saved.getId()).isPresent());
  }

  @Test
  void saveReturnsSameEntity(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    ProcessingRecord pr = rec("a@x.com", "h1", true);
    assertTrue(r.save(pr) == pr);
  }

  @Test
  void saveWithExistingIdUpdatesInPlace(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    ProcessingRecord pr = rec("a@x.com", "h1", true);
    r.save(pr);

    pr.setSuccess(false);
    r.save(pr);

    List<ProcessingRecord> recent = r.findRecentByEmailAddress("a@x.com", 10);
    assertEquals(1, recent.size());
    assertFalse(recent.get(0).isSuccess());
  }

  @Test
  void findRecentOnMissingKeyReturnsEmpty(@TempDir Path tmp) {
    assertTrue(repo(tmp.resolve("h.json"), 10).findRecentByEmailAddress("nobody@x.com", 5).isEmpty());
  }

  @Test
  void findRecentOnMissingFileReturnsEmpty(@TempDir Path tmp) {
    assertTrue(repo(tmp.resolve("nope.json"), 10).findRecentByEmailAddress("a@x.com", 5).isEmpty());
  }

  @Test
  void findRecentReturnsNewestFirst(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    r.save(rec("a@x.com", "h1", true));
    r.save(rec("a@x.com", "h2", false));
    r.save(rec("a@x.com", "h3", true));

    List<ProcessingRecord> recent = r.findRecentByEmailAddress("a@x.com", 3);
    assertEquals("h3", recent.get(0).getPdfHash());
    assertEquals("h2", recent.get(1).getPdfHash());
    assertEquals("h1", recent.get(2).getPdfHash());
  }

  @Test
  void findRecentHonorsLimit(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    r.save(rec("a@x.com", "h1", true));
    r.save(rec("a@x.com", "h2", true));
    r.save(rec("a@x.com", "h3", true));

    List<ProcessingRecord> recent = r.findRecentByEmailAddress("a@x.com", 2);
    assertEquals(2, recent.size());
    assertEquals("h3", recent.get(0).getPdfHash());
    assertEquals("h2", recent.get(1).getPdfHash());
  }

  @Test
  void perEmailCapEnforcedOnSave(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 3);
    for (int i = 0; i < 5; i++) {
      r.save(rec("a@x.com", "h" + i, true));
    }
    List<ProcessingRecord> recent = r.findRecentByEmailAddress("a@x.com", 10);
    assertEquals(3, recent.size());
    assertEquals("h4", recent.get(0).getPdfHash());
    assertEquals("h3", recent.get(1).getPdfHash());
    assertEquals("h2", recent.get(2).getPdfHash());
  }

  @Test
  void emailsAreIsolated(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    r.save(rec("a@x.com", "ha", true));
    r.save(rec("b@x.com", "hb", false));

    List<ProcessingRecord> recentA = r.findRecentByEmailAddress("a@x.com", 10);
    List<ProcessingRecord> recentB = r.findRecentByEmailAddress("b@x.com", 10);
    assertEquals(1, recentA.size());
    assertEquals("ha", recentA.get(0).getPdfHash());
    assertEquals(1, recentB.size());
    assertEquals("hb", recentB.get(0).getPdfHash());
    assertFalse(recentB.get(0).isSuccess());
  }

  @Test
  void persistsAcrossInstances(@TempDir Path tmp) {
    Path file = tmp.resolve("h.json");
    repo(file, 10).save(rec("a@x.com", "h1", true));

    List<ProcessingRecord> recent = repo(file, 10).findRecentByEmailAddress("a@x.com", 10);
    assertEquals(1, recent.size());
    assertEquals("h1", recent.get(0).getPdfHash());
  }

  @Test
  void findByIdAcrossEmails(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    ProcessingRecord a = rec("a@x.com", "ha", true);
    ProcessingRecord b = rec("b@x.com", "hb", false);
    r.save(a);
    r.save(b);

    Optional<ProcessingRecord> foundA = r.findById(a.getId());
    Optional<ProcessingRecord> foundB = r.findById(b.getId());
    assertTrue(foundA.isPresent());
    assertTrue(foundB.isPresent());
    assertEquals("ha", foundA.get().getPdfHash());
    assertEquals("hb", foundB.get().getPdfHash());
  }

  @Test
  void findByIdMissingReturnsEmpty(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    r.save(rec("a@x.com", "h1", true));
    assertTrue(r.findById(UUID.randomUUID()).isEmpty());
  }

  @Test
  void findByIdNullReturnsEmpty(@TempDir Path tmp) {
    assertTrue(repo(tmp.resolve("h.json"), 10).findById(null).isEmpty());
  }

  @Test
  void deleteRemovesEntry(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    ProcessingRecord pr = rec("a@x.com", "h1", true);
    r.save(pr);
    r.save(rec("a@x.com", "h2", true));

    r.delete(pr);

    List<ProcessingRecord> recent = r.findRecentByEmailAddress("a@x.com", 10);
    assertEquals(1, recent.size());
    assertEquals("h2", recent.get(0).getPdfHash());
    assertTrue(r.findById(pr.getId()).isEmpty());
  }

  @Test
  void deleteUnknownEntityIsNoOp(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    r.save(rec("a@x.com", "h1", true));

    ProcessingRecord phantom = rec("a@x.com", "h9", false);
    r.delete(phantom);

    assertEquals(1, r.findRecentByEmailAddress("a@x.com", 10).size());
  }

  @Test
  void creatingParentDirectoriesWorks(@TempDir Path tmp) {
    Path file = tmp.resolve("nested/dir/h.json");
    repo(file, 10).save(rec("a@x.com", "h1", true));
    assertTrue(Files.exists(file));
  }

  @Test
  void negativeLimitThrows(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    assertThrows(IllegalArgumentException.class, () -> r.findRecentByEmailAddress("a@x.com", -1));
  }

  @Test
  void zeroLimitReturnsEmpty(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    r.save(rec("a@x.com", "h1", true));
    assertTrue(r.findRecentByEmailAddress("a@x.com", 0).isEmpty());
  }

  @Test
  void zeroCapRejected(@TempDir Path tmp) {
    assertThrows(IllegalArgumentException.class,
        () -> new JsonFileProcessingHistoryRepository(tmp.resolve("h.json"), 0));
  }

  @Test
  void timestampRoundTrips(@TempDir Path tmp) {
    JsonFileProcessingHistoryRepository r = repo(tmp.resolve("h.json"), 10);
    Instant ts = Instant.parse("2026-04-19T10:15:30Z");
    r.save(new ProcessingRecord("a@x.com", "h", true, ts));

    List<ProcessingRecord> recent = r.findRecentByEmailAddress("a@x.com", 1);
    assertEquals(ts, recent.get(0).getTimestamp());
  }
}
