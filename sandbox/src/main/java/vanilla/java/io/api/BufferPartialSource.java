package vanilla.java.io.api;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * @author peter.lawrey
 */
public interface BufferPartialSource extends Closeable {
    /**
     * @param in ByteBuffer to consume some or all data from
     */
    public void consume(ByteBuffer in);

    public void close();
}
