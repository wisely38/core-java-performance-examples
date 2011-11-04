package vanilla.java.io.api;

/**
 * @author peter.lawrey
 */
public interface BufferSession extends BufferPartialSource {
    /**
     * @param sink Sink to send events and replies to.
     */
    public void setSink(BufferSink sink);
}
