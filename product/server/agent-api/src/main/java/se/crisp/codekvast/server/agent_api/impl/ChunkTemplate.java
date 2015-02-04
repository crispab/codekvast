package se.crisp.codekvast.server.agent_api.impl;

import lombok.RequiredArgsConstructor;
import se.crisp.codekvast.server.agent_api.AgentApiException;

import java.util.List;

/**
 * A template for chunked processing of lists.
 *
 * @author Olle Hallin
 */
@RequiredArgsConstructor
public abstract class ChunkTemplate<V> {

    private final int chunkSize;
    private final List<? extends V> values;

    public int execute() throws AgentApiException {
        int from = 0;
        int chunkNumber = 1;
        int processed = 0;
        int size = values.size();

        while (from < size) {
            int to = Math.min(from + chunkSize, size);

            doWithChunk(values.subList(from, to), chunkNumber);

            processed += (to - from);
            from = to;
            chunkNumber += 1;
        }

        assert processed == size : String.format("Bad chunk logic, not all elements were processed. Expected: %d, actual: %d", size,
                                                 processed);
        return processed;
    }

    /**
     * Process one chunk of the list
     *
     * @param chunk       The sublist to process
     * @param chunkNumber The chunk number
     */
    public abstract void doWithChunk(List<? extends V> chunk, int chunkNumber) throws AgentApiException;
}
