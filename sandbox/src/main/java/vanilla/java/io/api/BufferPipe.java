package vanilla.java.io.api;

/**
 * @author peter.lawrey
 */
public interface BufferPipe extends BufferSink {
    /**
     * @param source the source listening to data from this pipe.
     */
    public void setSource(BufferSource source);
}
