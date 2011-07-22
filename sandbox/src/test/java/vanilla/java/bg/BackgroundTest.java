package vanilla.java.bg;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

// /tmp: Average/90/99/99.9/99.99% times were 335 / 386 / 960 / 1,138 / 4,040
// /ssd: Average/90/99/99.9/99.99% times were 329 / 377 / 975 / 1,028 / 7,085
// /nfs: Average/90/99/99.9/99.99% times were 346 / 390 / 978 / 1,105 / 3,559
public class BackgroundTest {

    @Test
    public void testFile() throws InterruptedException, IOException {
        File file = new File("/home/peter/deleteme.dat");
        file.deleteOnExit();
        BackgroundDataOutput backgroundChannel = new BackgroundChannel(file);
        File file2 = new File("/home/peter/deleteme2.dat");
        file2.deleteOnExit();
        BackgroundDataOutput backgroundChannel2 = new BackgroundChannel(file2);
        byte[] bytes = new byte[128];
        long[] times = new long[100000];
        for (int i = -10000; i < times.length; i += 2) {
            if (i % 5 == 4) {
                Thread.sleep(1);
            } else {
                Thread.yield();
            }
            try {
                long start = System.nanoTime();
                backgroundChannel.write(bytes);
                long time1 = System.nanoTime() - start;
                if (i >= 0) {
                    times[i] = time1;
                }
                long start2 = System.nanoTime();
                backgroundChannel2.write(bytes);
                long time2 = System.nanoTime() - start2;
                if (i >= 0) {
                    times[i + 1] = time2;
                }
            } catch (Exception e) {
                System.out.printf("After %,d%n", i);
                e.printStackTrace();
                break;
            }
        }
        backgroundChannel.close();
        backgroundChannel2.close();
        Arrays.sort(times);
        System.out.printf("Average/90/99/99.9/99.99%% times were %,d / %,d / %,d / %,d / %,d %n",
                times[times.length / 2],
                times[times.length * 9 / 10],
                times[times.length - times.length / 100],
                times[times.length - times.length / 1000 - 1],
                times[times.length - times.length / 10000 - 1]
        );
    }

    @Test
    public void testSocket() throws IOException, InterruptedException {
        final ServerSocket ss = new ServerSocket(0);
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Socket s = ss.accept();
                    InputStream in = s.getInputStream();
                    long count = 0;
                    byte[] bytes = new byte[1024];
                    int len;
                    while ((len = in.read(bytes)) >= 0) count+=len;
                    in.close();
                    s.close();
                    System.out.println("Read " + count + " bytes.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        InetSocketAddress remote = new InetSocketAddress("localhost", ss.getLocalPort());
        SocketChannel sc = SocketChannel.open(remote);
        BackgroundDataOutput scb = new BackgroundChannel(remote.toString(), sc, 32 * 1024);

        byte[] bytes = new byte[100];
        long[] times = new long[240000];
        for (int i = -10000; i < times.length; i ++) {
            if (i % 5 == 4) {
                Thread.sleep(1);
            } else {
                Thread.yield();
            }
            try {
                long start = System.nanoTime();
                scb.write(bytes);
                long time = System.nanoTime() - start;
                if (i >= 0) {
                    times[i] = time;
                }
            } catch (Exception e) {
                System.out.printf("After %,d%n", i);
                e.printStackTrace();
                break;
            }
        }
        scb.close();

        Arrays.sort(times);
        System.out.printf("best/Average/90/99/99.9/99.99%% times were %,d / %,d / %,d / %,d / %,d / %,d %n",
                times[0],
                times[times.length / 2],
                times[times.length * 9 / 10],
                times[times.length - times.length / 100],
                times[times.length - times.length / 1000 - 1],
                times[times.length - times.length / 10000 - 1]
        );
        t.join();
    }
}
