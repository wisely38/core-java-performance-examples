package vanilla.java.io;

import vanilla.java.io.api.BufferPartialSource;
import vanilla.java.io.api.BufferPipe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author peter.lawrey
 */
public class DirectPipe implements BufferPipe<BufferPartialSource> {
    private final ByteBuffer bb;
    private BufferPartialSource source = null;

    public DirectPipe(int capacity) {
        bb = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    @Override
    public void setSource(BufferPartialSource source) {
        this.source = source;
    }

    @Override
    public ByteBuffer acquireByteBuffer(int capacity) {
        if (bb.remaining() < capacity)
            throw new IllegalStateException();
        return bb;
    }

    @Override
    public void release(ByteBuffer bb) {
        bb.flip();
        if (source != null)
            source.consume(bb);
        if (bb.remaining() > 0) {
            if (bb.remaining() == bb.capacity())
                throw new IllegalStateException("Source " + source + " failed to consume data.");
            bb.compact();
        } else {
            bb.clear();
        }
    }

    @Override
    public void close() {
        if (source != null)
            source.close();
    }
}
