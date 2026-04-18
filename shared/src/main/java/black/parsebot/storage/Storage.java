package black.parsebot.storage;

import java.io.IOException;
import java.util.List;

public interface Storage<T> {

  void append(T item);

  List<T> readAll() throws IOException;

  List<T> readLast(int n) throws IOException;
}
