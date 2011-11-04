package vanilla.java.io;

import org.junit.Test;

/**
 * @author peter.lawrey
 */
public class DirectPipeTest {
    @Test
    public void testPerf() {
        for (int i = 0; i < 5; i++) {
            IOPerfTests.testThroughput(new DirectPipe(1024));
            IOPerfTests.testLatency(new DirectPipe(1024), 1, 1000);
        }
    }
}
