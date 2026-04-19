package black.parsebot.persistence;

import java.util.List;
import java.util.UUID;

public interface ProcessingHistoryRepository extends Repository<ProcessingRecord, UUID> {

  List<ProcessingRecord> findRecentByEmailAddress(String emailAddress, int limit);
}
