package vanilla.java.io;

import vanilla.java.io.api.BufferPipe;
import vanilla.java.io.api.BufferSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pipe which passes via a socket.
 *
 * @author peter.lawrey
 */
public class SocketPipe implements BufferPipe {
    private static final int DEFAULT_CAPACITY = 32 * 1024;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final ByteBuffer bb;
    private final SocketChannel sink;
    private BufferSource source = null;

    public SocketPipe() throws IOException {
        bb = createBuffer();
        final ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(0));
        service.execute(new Runnable() {
            @Override
            public void run() {
                ByteBuffer bb = createBuffer();
                SocketChannel pipeSource = null;
                try {
                    pipeSource = SocketChannel.open(new InetSocketAddress("localhost", ssc.socket().getLocalPort()));
                    while (!Thread.interrupted()) {
                        int len = pipeSource.read(bb);
                        if (len < 0)
                            break;
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
                    if (pipeSource != null)
                        try {
                            pipeSource.close();
                        } catch (IOException ignored) {
                        }
                    close();
                    if (source != null)
                        source.close();
                }
            }
        });
        this.sink = ssc.accept();
        ssc.close();
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
        service.shutdown();
        try {
            sink.close();
        } catch (IOException ignored) {
        }
    }
}
