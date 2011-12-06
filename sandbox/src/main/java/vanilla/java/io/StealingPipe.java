package vanilla.java.io;

import vanilla.java.io.api.BufferPipe;
import vanilla.java.io.api.BufferSource;
import vanilla.java.lang.PaddedAtomicLong;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author peter.lawrey
 */
public class StealingPipe implements BufferPipe {
    private static final int DEFAULT_CAPACITY = 64 * 1024;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final ByteBuffer writeBuffer = createBuffer();
    private final PaddedAtomicLong writeCount = new PaddedAtomicLong();
    private final PaddedAtomicLong writeBatch = new PaddedAtomicLong();
    private final PaddedAtomicLong pad = new PaddedAtomicLong();
    private final PaddedAtomicLong readCount = new PaddedAtomicLong();
    private final PaddedAtomicLong readBatch = new PaddedAtomicLong();
    private final ByteBuffer readBuffer = writeBuffer.slice().order(ByteOrder.nativeOrder());

    private BufferSource source = null;

    public StealingPipe() throws IOException {
        service.execute(new Runnable() {
            @Override
            public void run() {
                SocketChannel pipeSource = null;
                int lastBatch = -1;
                try {
                    LOOP:
                    while (!Thread.interrupted()) {
                        // do we need to rewind?
                        if (writeBatch.get() != readBatch.get()) {
                            readCount.set(0);
                            readBatch.incrementAndGet();
                            if (writeBatch.get() != readBatch.get()) {
                                throw new IllegalStateException();
                            }
                        }
                        long rc = readCount.get(), wc;
                        // is there more to read?
                        while (((wc = writeCount.get()) - rc) <= 0) {
                            if (service.isShutdown())
                                break LOOP;
                            if (readBatch.get() != writeBatch.get())
                                continue LOOP;
                        }
                        readBuffer.limit((int) wc);
                        readBuffer.position((int) rc);

                        source.consume(readBuffer);
                        if (readBuffer.remaining() != 0)
                            throw new AssertionError(source + " failed to read all");

                        readCount.set(wc);
                    }
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
//                    System.out.println("Finished");
                    readBatch.set(Integer.MAX_VALUE);
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
        if (writeBuffer.remaining() < capacity + 1) {
            rollWriteBatch();
        }
        return writeBuffer;
    }

    private void rollWriteBatch() {
        long wc = writeCount.get();
        // reader has to catch up.
        while (readBatch.get() < writeBatch.get() ||
                wc - readCount.get() > 0) ;
        // rewind.
        writeBuffer.clear();
        writeCount.set(0);
        writeBatch.incrementAndGet();
    }

    @Override
    public void release(ByteBuffer bb) {
        writeCount.set(bb.position());
    }

    @Override
    public void close() {
        rollWriteBatch();
        service.shutdown();
    }
}
