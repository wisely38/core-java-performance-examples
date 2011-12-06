package vanilla.java.io;

import vanilla.java.io.api.BufferPipe;
import vanilla.java.io.api.BufferSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pipe using NIO.
 *
 * @author peter.lawrey
 */
public class NioPipe implements BufferPipe {
    private static final int DEFAULT_CAPACITY = 32 * 1024;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final ByteBuffer bb;
    private final Pipe pipe;
    private final Pipe.SinkChannel sink;
    private BufferSource source = null;

    public NioPipe() throws IOException {
        bb = createBuffer();
        this.pipe = Pipe.open();
        sink = pipe.sink();
        service.execute(new Runnable() {
            @Override
            public void run() {
                ByteBuffer bb = createBuffer();
                Pipe.SourceChannel pipeSource = pipe.source();
                try {
                    while (!Thread.interrupted()) {
                        pipeSource.read(bb);
                        bb.flip();
                        if (source != null)
                            source.consume(bb);
                        final int remaining = bb.remaining();
                        if (remaining == bb.capacity())
                            throw new IllegalStateException("Source " + source + " failed to consume.");
                        if (remaining > 0)
                            bb.compact();
                        else
                            bb.clear();
                    }
                } catch (ClosedChannelException ignored) {

                } catch (Exception e) {
                    e.printStackTrace();

                } finally {
                    close();
                    if (source != null)
                        source.close();
                }
            }
        });
    }

    private static ByteBuffer createBuffer() {
        return ByteBuffer.allocateDirect(DEFAULT_CAPACITY).order(ByteOrder.nativeOrder());
    }

    @Override
    public void setSource(BufferSource source) {
        this.source = source;
    }

    @Override
    public ByteBuffer acquireByteBuffer(int capacity) {
        return bb;
    }

    @Override
    public void release(ByteBuffer bb) {
        bb.flip();
        try {
            sink.write(bb);
            if (bb.remaining() > 0)
                bb.compact();
            else
                bb.clear();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        try {
            pipe.sink().close();
        } catch (IOException ignored) {
        }
        service.shutdown();
        try {
            pipe.source().close();
        } catch (IOException ignored) {
        }
    }
}
