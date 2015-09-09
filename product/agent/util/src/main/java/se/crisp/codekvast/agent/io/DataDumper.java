package se.crisp.codekvast.agent.io;

import se.crisp.codekvast.agent.model.Jvm;

import java.util.Set;

/**
 * Strategy for dumping collection data.
 */
public interface DataDumper {

    /**
     * Make preparations for dumping data, such as creating output directories.
     *
     * @return true iff the preparation was successful.
     */
    boolean prepareForDump();

    /**
     * Dump the data.
     *
     * @param jvm                              The JVM data.
     * @param dumpCount                        The dump counter.
     * @param recordingIntervalStartedAtMillis When the recording of these invocations started.
     * @param invocations                      The set of invocations to dump.
     */
    void dumpData(Jvm jvm, int dumpCount, long recordingIntervalStartedAtMillis, Set<String> invocations);
}
