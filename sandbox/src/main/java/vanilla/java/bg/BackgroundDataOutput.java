package vanilla.java.bg;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BackgroundDataOutput implements BackgroundTask, DataOutput, Closeable, Flushable {
    protected final String source;
    protected final int capacity;
    protected final Lock lock = new ReentrantLock();
    protected final ByteBuffer[] buffers;
    private volatile int writeBuffer = 0;
    protected volatile boolean closed = false;

    public BackgroundDataOutput(String source, int capacity) {
        this.source = source;
        this.capacity = capacity;
        buffers = new ByteBuffer[]{ByteBuffer.allocateDirect(capacity), ByteBuffer.allocateDirect(capacity)};
    }

    public boolean perform() {
        ByteBuffer wb = null;
        if(!lock.tryLock()) return true;
        try {
            wb = writeBuffer();
            if (wb.position() > 0) {
                swapBuffers();
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
        writeData(wb);
        return true;
    }

    protected abstract void writeData(ByteBuffer wb);

    private void swapBuffers() {
        writeBuffer ^= 1;
    }

    public ByteBuffer writeBuffer() {
        return buffers[writeBuffer];
    }

    public void write(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    public void write(byte[] bytes, int offset, int length) {
        if (length > capacity)
            throw new IllegalArgumentException(source + ": byte[] larger than capacity " + bytes.length + " > " + capacity);
        lock.lock();
        try {
            ensureAvailable(bytes.length);
            writeBuffer().put(bytes, offset, length);

        } finally {
            lock.unlock();
        }
    }

    public void write(ByteBuffer bytes) {
        int remaining = bytes.remaining();
        if (remaining > capacity)
            throw new IllegalArgumentException(source + ": ByteBuffer larger than capacity " + remaining + " > " + capacity);
        lock.lock();
        try {
            ensureAvailable(remaining);
            writeBuffer().put(bytes);

        } finally {
            lock.unlock();
        }
    }

    protected void ensureAvailable(int remaining) {
        while (writeBuffer().remaining() < remaining) {
            lock.unlock();
            Background.wake();
            Thread.yield();
            lock.lock();
        }
    }

    public void write(int b) {
        lock.lock();
        try {
            ensureAvailable(1);
            writeBuffer().put((byte) b);

        } finally {
            lock.unlock();
        }
    }

    public void writeBoolean(boolean v) {
        lock.lock();
        try {
            ensureAvailable(1);
            writeBuffer().put(v ? 1 : (byte) 0);

        } finally {
            lock.unlock();
        }
    }

    public void writeByte(int v) {
        write(v);
    }

    public void writeShort(int v) {
        lock.lock();
        try {
            ensureAvailable(2);
            writeBuffer().putShort((short) v);

        } finally {
            lock.unlock();
        }
    }

    public void writeChar(int v) {
        lock.lock();
        try {
            ensureAvailable(2);
            writeBuffer().putChar((char) v);

        } finally {
            lock.unlock();
        }
    }

    public void writeInt(int v) {
        lock.lock();
        try {
            ensureAvailable(4);
            writeBuffer().putInt(v);

        } finally {
            lock.unlock();
        }
    }

    public void writeLong(long v) {
        lock.lock();
        try {
            ensureAvailable(8);
            writeBuffer().putLong(v);

        } finally {
            lock.unlock();
        }
    }

    public void writeFloat(float v) {
        lock.lock();
        try {
            ensureAvailable(4);
            writeBuffer().putFloat(v);

        } finally {
            lock.unlock();
        }
    }

    public void writeDouble(double v) {
        lock.lock();
        try {
            ensureAvailable(8);
            writeBuffer().putDouble(v);

        } finally {
            lock.unlock();
        }
    }

    public void writeBytes(String s) {
        if (s.length() > capacity)
            throw new IllegalArgumentException("String larger than capacity " + s.length() + " > " + capacity);
        lock.lock();
        try {
            ensureAvailable(s.length());
            ByteBuffer wb = writeBuffer();
            for (int i = 0; i < s.length(); i++)
                wb.put((byte) s.charAt(i));

        } finally {
            lock.unlock();
        }
    }

    public void writeChars(String s) {
        if (s.length() * 2 > capacity)
            throw new IllegalArgumentException("String larger than capacity " + s.length() * 2 + " > " + capacity);
        lock.lock();
        try {
            ensureAvailable(s.length());
            ByteBuffer wb = writeBuffer();
            for (int i = 0; i < s.length(); i++)
                wb.putChar(s.charAt(i));

        } finally {
            lock.unlock();
        }
    }

    public void writeUTF(String s) {
        try {
            write(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public void flush() {
        lock.lock();
        try {
            while (writeBuffer().position() > 0) {
                lock.unlock();
                Background.wake();
                Thread.yield();
                lock.lock();
            }
        } finally {
            lock.unlock();
        }
    }

    public void close() throws IOException {
        if (closed) return;
        closed = true;
        flush();
        Background.unregister(this);
        doClose();
    }

    protected abstract void doClose() throws IOException;
}
