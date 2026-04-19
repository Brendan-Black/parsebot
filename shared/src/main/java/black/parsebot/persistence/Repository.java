package black.parsebot.persistence;

import java.util.Optional;

public interface Repository<E, ID> {

  E save(E entity);

  Optional<E> findById(ID id);

  void delete(E entity);
}
