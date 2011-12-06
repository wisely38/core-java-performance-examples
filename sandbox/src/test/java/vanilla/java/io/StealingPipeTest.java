package vanilla.java.io;

import org.junit.Test;
import vanilla.java.lang.HiresTimer;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class StealingPipeTest {
    @Test
    public void testPerf() throws IOException {
        int delay = 100;
        for (int i = 0; i < 6; i++) {
            IOPerfTests.testLatency(new StealingPipe(), 5, delay);
            delay = IOPerfTests.testThroughput(new StealingPipe());
            HiresTimer.init();
        }
    }
}
