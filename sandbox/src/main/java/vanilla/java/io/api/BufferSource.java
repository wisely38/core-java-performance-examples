package vanilla.java.io.api;

import java.nio.ByteBuffer;

/**
 * @author peter.lawrey
 */
public interface BufferSource extends BufferPartialSource {
    /**
     * @param in ByteBuffer to consume all data from
     */
    public void consume(ByteBuffer in);
}
