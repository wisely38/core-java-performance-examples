package vanilla.java.io.api;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * @author peter.lawrey
 */
public interface BufferSink extends Closeable {
    public ByteBuffer acquireByteBuffer(int capacity);

    public void release(ByteBuffer bb);

    public void close();
}
