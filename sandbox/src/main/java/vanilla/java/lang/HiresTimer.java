package vanilla.java.lang;

/**
 * @author peter.lawrey
 */
public enum HiresTimer {
    ;
    private static int factor;
    private static Thread TIMER_THREAD = null;
    public static final int FACTOR_PRECISION_BITS = 10;

    static class CounterHolder {
        private static final PaddedAtomicLong counter = new PaddedAtomicLong();
    }

    static {
        init();
        init();
    }

    public static long nanoTime() {
        return (CounterHolder.counter.get() * factor) >> FACTOR_PRECISION_BITS;
    }

    public static void init() {
        do {
            synchronized (HiresTimer.class) {
                Thread t = TIMER_THREAD;
                if (t == null || !t.isAlive()) {
                    t = new Thread(new Runnable() {
                        public void run() {
                            long counter = 0;
                            while (!Thread.interrupted())
                                CounterHolder.counter.lazySet(counter++);
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                    TIMER_THREAD = t;
                }
            }
            Thread.yield();
        } while (CounterHolder.counter.get() < 1);

        long start0 = System.nanoTime();
        long start;
        while ((start = System.nanoTime()) == start0) ;
        long counter1 = CounterHolder.counter.get();
        if (counter1 == 0) throw new IllegalStateException("thread not started.");
        long end0 = start + 50 * 1000 * 1000; // 50 ms
        long end;
        while ((end = System.nanoTime()) < end0) ;
        long counter2 = CounterHolder.counter.get();
        factor = (int) (((end - start) << FACTOR_PRECISION_BITS) / (counter2 - counter1));
        System.out.printf("Each count takes %.2f ns%n", (double) factor / (1 << FACTOR_PRECISION_BITS));
    }

    public static void stop() {
        final Thread t = TIMER_THREAD;
        if (t != null)
            t.interrupt();
    }
}
