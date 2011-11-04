package vanilla.java.io;

import org.junit.Test;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class SocketPipeTest {
    @Test
    public void testPerf() throws IOException {
        for (int i = 0; i < 5; i++) {
            IOPerfTests.testThroughput(new SocketPipe());
            IOPerfTests.testLatency(new SocketPipe(), 100, 1000);
        }
    }
}
