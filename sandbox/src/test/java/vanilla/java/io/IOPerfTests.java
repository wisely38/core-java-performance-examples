package vanilla.java.io;

import vanilla.java.io.api.BufferPipe;
import vanilla.java.io.api.BufferSource;
import vanilla.java.lang.HiresTimer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author peter.lawrey
 */
public enum IOPerfTests {
    ;

    static {
        HiresTimer.init();
    }

    public static int testThroughput(BufferPipe pipe) {
        final AtomicLong lastExpected = new AtomicLong(-1);
        pipe.setSource(new BufferSource() {
            long expected = 0;

            @Override
            public void consume(ByteBuffer in) {
                while (in.remaining() >= 16) {
                    long num = in.getLong();
                    if (num != expected) {
                        lastExpected.set(Long.MAX_VALUE);
                        throw new AssertionError("Expected " + expected + " but got " + Long.toHexString(num));
                    }
                    long num2 = in.getLong();
                    if (num != num2) {
                        lastExpected.set(Long.MAX_VALUE);
                        throw new AssertionError("Mismatch " + num + " != " + num2);
                    }
                    expected++;
                }
                lastExpected.lazySet(expected);
            }

            @Override
            public void close() {
                lastExpected.set(expected);
            }
        });

        long start = System.nanoTime();
        long end = start + 2L * 1000 * 1000 * 1000;
        long count = 0;
        do {
            for (int i = 0; i < 1000 * 1000; i++) {
                final ByteBuffer bb = pipe.acquireByteBuffer(16);
                bb.putLong(count);
                bb.putLong(count++);
                pipe.release(bb);
            }
        } while (end > System.nanoTime());
        while (lastExpected.get() < count) ;
        pipe.close();
        long time = System.nanoTime() - start;
        System.out.printf("%s: Throughput %.1f M msg/s%n", nameOf(pipe), count * 1000.0 / time);
        return (int) (time / count + 1);
    }

    public static void testLatency(BufferPipe pipe, final int latencyRes, int pauseNS) {
        final AtomicLong lastExpected = new AtomicLong(-1);
        final int[] latencies = new int[20 * 1000 + 1];

        pipe.setSource(new BufferSource() {
            long expected = 0;

            @Override
            public void consume(ByteBuffer in) {
                while (in.remaining() >= 16) {
                    long num = in.getLong();
                    if (num != expected) {
                        lastExpected.set(Long.MAX_VALUE);
                        throw new AssertionError("Expected " + expected + " but got " + num);
                    }
                    expected++;
                    long sentTime = in.getLong();
                    int latency = (int) (HiresTimer.nanoTime() - sentTime) / latencyRes;
                    if (latency >= latencies.length)
                        latency = latencies.length - 1;
                    latencies[latency]++;
                }
                lastExpected.lazySet(expected);
            }

            @Override
            public void close() {
                lastExpected.set(expected);
            }
        });

        long start = System.nanoTime();
        long end = start + 20L * 1000 * 1000 * 1000;
        long count = 0;
        do {
            for (int i = 0; i < 1000; i++) {
                final ByteBuffer bb = pipe.acquireByteBuffer(16);
                bb.putLong(count++);
                bb.putLong(HiresTimer.nanoTime());
                pipe.release(bb);
                // pause briefly.
                long endPause = HiresTimer.nanoTime() + pauseNS;
                while (HiresTimer.nanoTime() < endPause) ;
            }
        } while (end > System.nanoTime());
        pipe.close();

        // estimate cost of HiresTimer.
/*
        long start2 = HiresTimer.nanoTime();
        long end2 = 0;
        for (int i = 1; i < 1000 * 1000; i++)
            end2 = HiresTimer.nanoTime();
        long overhead = (end2 - start2) / 1000 / 1000;
*/

        while (lastExpected.get() < count) ;
        pipe.close();

        System.out.printf("%s: Latency for 50/99/99.9/99.99 %%tile is %s / %s / %s / %s μs%n",
                nameOf(pipe),
                asString(fromEnd(latencies, count / 2) * latencyRes / 1000.0),
                asString(fromEnd(latencies, count / 100) * latencyRes / 1000.0),
                asString(fromEnd(latencies, count / 1000) * latencyRes / 1000.0),
                asString(fromEnd(latencies, count / 10000) * latencyRes / 1000.0)
        );
    }

    private static String asString(double v) {
        if (v < 10)
            return String.valueOf((long) (v * 1000 + 0.5) / 1e3);
        if (v < 100)
            return String.valueOf((long) (v * 100 + 0.5) / 1e2);
        if (v < 1000)
            return String.valueOf((long) (v * 10 + 0.5) / 1e1);
        return Long.toString((long) (v + 0.5));
    }

    private static String nameOf(Object o) {
        try {
            if (o.getClass().getMethod("toString").getDeclaringClass() == Object.class)
                return o.getClass().getSimpleName();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        return o.toString();
    }

    private static int fromEnd(int[] latencies, long total) {
        for (int i = latencies.length - 1; i > 0; i--) {
            total -= latencies[i];
            if (total <= 0)
                return i;
        }
        return 0;
    }
}
