package vanilla.java.io.api;

import java.io.Closeable;

/**
 * @author peter.lawrey
 */
public interface Factory<T> extends Closeable {
    public T create();

    public void close();
}
