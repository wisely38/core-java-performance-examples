package vanilla.java.lang;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter.lawrey
 */
public class PaddedAtomicInteger extends AtomicInteger {
    public long p2, p3, p4, p5, p6, p7;

    public long $sum$() {
        return p2 + p3 + p4 + p5 + p6 + p7;
    }
}
