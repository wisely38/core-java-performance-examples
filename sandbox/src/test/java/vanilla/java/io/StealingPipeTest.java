package vanilla.java.io;

import org.junit.Test;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class StealingPipeTest {
    @Test
    public void testPerf() throws IOException {
        for (int i = 0; i < 6; i++) {
            IOPerfTests.testThroughput(new StealingPipe());
            IOPerfTests.testLatency(new StealingPipe(), 5, 60);
        }
    }
}
