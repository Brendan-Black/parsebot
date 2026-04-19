package black.parsebot.persistence;

import java.io.IOException;
import java.util.List;

public interface PseudoPersistence<T> {

  void append(T item);

  List<T> readAll() throws IOException;

  List<T> readLast(int n) throws IOException;
}
