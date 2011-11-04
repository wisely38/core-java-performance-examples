package vanilla.java.lang;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author peter.lawrey
 */
public class PaddedAtomicLong extends AtomicLong {
    public long p2, p3, p4, p5, p6, p7;

    public long $sum$() {
        return p2 + p3 + p4 + p5 + p6 + p7;
    }
}
