package vanilla.java.lang;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter.lawrey
 */
public class PaddedAtomicLong2 {
    private static final Unsafe unsafe = getUnsafe();

    static Unsafe getUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static final long valueOffset;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset(AtomicInteger.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    public long p1, p2, p3, p4, p5, p6, p7;
    private volatile long value;
    public long q1, q2, q3, q4, q5, q6, q7;

    public long $sum$() {
        return p1 + p2 + p3 + p4 + p5 + p6 + p7
                + q1 + q2 + q3 + q4 + q5 + q6 + q7;
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
    }

    public void lazySet(long value) {
        unsafe.putOrderedLong(this, valueOffset, value);
    }

    public final long getAndSet(long newValue) {
        for (; ; ) {
            long current = get();
            if (compareAndSet(current, newValue))
                return current;
        }
    }

    public final boolean compareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    public final long getAndIncrement() {
        while (true) {
            long current = get();
            long next = current + 1;
            if (compareAndSet(current, next))
                return current;
        }
    }

    public String toString() {
        return Long.toString(get());
    }

    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    public final long incrementAndGet() {
        for (; ; ) {
            long current = get();
            long next = current + 1;
            if (compareAndSet(current, next))
                return next;
        }
    }
}
