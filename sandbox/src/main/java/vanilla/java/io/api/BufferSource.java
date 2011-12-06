package vanilla.java.io.api;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * @author peter.lawrey
 */
public interface BufferSource extends Closeable {
    /**
     * @param in ByteBuffer to consume some or all data from, any data not consumed is presented next time.
     */
    public void consume(ByteBuffer in);

    public void close();
}
