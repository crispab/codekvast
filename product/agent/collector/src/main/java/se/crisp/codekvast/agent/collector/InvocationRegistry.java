package se.crisp.codekvast.agent.collector;

import org.aspectj.lang.Signature;
import se.crisp.codekvast.agent.lib.config.CollectorConfig;
import se.crisp.codekvast.agent.lib.io.InvocationDataDumper;
import se.crisp.codekvast.agent.lib.model.Jvm;
import se.crisp.codekvast.agent.lib.util.ComputerID;
import se.crisp.codekvast.agent.lib.util.SignatureUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This is the target of the method execution recording aspects.
 * <p>
 * It holds data about method invocations, and methods for outputting the invocation data to disk.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("Singleton")
public class InvocationRegistry {

    public static InvocationRegistry instance;

    private final Jvm jvm;
    private final InvocationDataDumper invocationDataDumper;

    // Toggle between two invocation sets to avoid synchronisation
    private final Set[] invocations = new Set[2];
    private volatile int currentInvocationIndex = 0;
    private long recordingIntervalStartedAtMillis = System.currentTimeMillis();

    public InvocationRegistry(Jvm jvm, InvocationDataDumper invocationDataDumper) {
        this.jvm = jvm;
        this.invocationDataDumper = invocationDataDumper;

        for (int i = 0; i < invocations.length; i++) {
            this.invocations[i] = new ConcurrentSkipListSet<String>();
        }
    }

    /**
     * Should be called before handing over to the AspectJ load-time weaver, or else nothing will be registered.
     *
     * @param config The collector configuration. May not be null.
     * @param invocationDataDumper The strategy for dumping invocation data to the outside world.
     */
    public static void initialize(CollectorConfig config, InvocationDataDumper invocationDataDumper) {
        if (config == null) {
            instance = null;
            return;
        }

        String version = InvocationRegistry.class.getPackage().getImplementationVersion();
        if (version == null || version.trim().isEmpty()) {
            version = "dev-vcsId";
        }

        int dash = version.lastIndexOf("-");
        String collectorVersion = version.substring(0, dash);
        String collectorVcsId = version.substring(dash + 1);

        InvocationRegistry.instance = new InvocationRegistry(
                Jvm.builder()
                   .collectorVersion(collectorVersion)
                   .collectorVcsId(collectorVcsId)
                   .collectorConfig(config)
                   .computerId(ComputerID.compute().toString())
                   .hostName(getHostName())
                   .jvmUuid(UUID.randomUUID().toString())
                   .startedAtMillis(System.currentTimeMillis())
                   .build(),
                invocationDataDumper);
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    /**
     * Record that this method signature was invoked at current recording interval.
     * <p>
     * Thread-safe.
     *
     * @param signature The captured method invocation signature.
     */
    public void registerMethodInvocation(Signature signature) {
        //noinspection unchecked
        invocations[currentInvocationIndex].add(SignatureUtils.signatureToString(signature, false));
    }

    /**
     * Dumps method invocations to a file on disk.
     * <p>
     * Thread-safe.
     *
     * @param dumpCount the ordinal number of this dump. Is used in a comment in the dump file.
     */
    public void dumpData(int dumpCount) {
        if (!invocationDataDumper.prepareForDump()) {
            CodekvastCollector.out.println("Cannot dump invocation data");
        } else {
            long oldRecordingIntervalStartedAtMillis = recordingIntervalStartedAtMillis;
            int oldIndex = currentInvocationIndex;

            toggleInvocationsIndex();

            //noinspection unchecked
            invocationDataDumper.dumpData(jvm, dumpCount, oldRecordingIntervalStartedAtMillis, invocations[oldIndex]);

            invocations[oldIndex].clear();
        }
    }

    private void toggleInvocationsIndex() {
        recordingIntervalStartedAtMillis = System.currentTimeMillis();
        currentInvocationIndex = currentInvocationIndex == 0 ? 1 : 0;
    }

}
