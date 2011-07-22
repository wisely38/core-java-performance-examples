package vanilla.java.bg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackgroundChannel extends BackgroundDataOutput {
    public static final Logger LOGGER = Logger.getLogger(BackgroundChannel.class.getName());

    private final ByteChannel channel;

    public BackgroundChannel(File file) throws IOException {
        this(file, 128 * 1024);
    }

    public BackgroundChannel(File file, int capacity) throws IOException {
        this(file.toString(), new FileOutputStream(file).getChannel(), capacity);
    }

    public BackgroundChannel(String source, ByteChannel channel, int capacity) throws IOException {
        super(source, capacity);
        this.channel = channel;
        Background.register(this);
    }

    protected void writeData(ByteBuffer wb) {
        // do something with the data
        try {
            wb.flip();
            do {
                channel.write(wb);
            } while (wb.remaining() > 0);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, source + ": Unable to write", e);
        } finally {
            wb.clear();
        }
    }

    protected void doClose() throws IOException {
        channel.close();
    }
}
