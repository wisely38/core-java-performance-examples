package vanilla.java.io.api;

/**
 * @author peter.lawrey
 */
public interface BufferPipe<S extends BufferPartialSource> extends BufferSink {
    /**
     * @param source the source listening to data from this pipe.
     */
    public void setSource(S source);
}
