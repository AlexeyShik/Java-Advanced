package info.kgeorgiy.ja.shik.walk;

import java.io.IOException;

@FunctionalInterface
public interface BiWalkConsumer<T, U> {
    void accept(T t, U u) throws IOException;
}
