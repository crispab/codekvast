/*
 * Copyright (c) 2015-2017 Crisp AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.agent.collector;

import io.codekvast.agent.collector.io.CodekvastPublishingException;
import io.codekvast.agent.collector.io.InvocationDataPublisher;
import io.codekvast.agent.lib.config.CollectorConfig;
import io.codekvast.agent.lib.util.SignatureUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.Signature;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is the target of the method execution recording aspects.
 * <p>
 * It holds data about method invocations and methods for publishing the data.
 *
 * @author olle.hallin@crisp.se
 */
@SuppressWarnings("Singleton")
@Slf4j
public class InvocationRegistry {

    @SuppressWarnings("StaticInitializerReferencesSubClass")
    public static InvocationRegistry instance = new NullInvocationRegistry();

    // Toggle between two invocation sets to avoid synchronisation
    private final Set[] invocations;
    private volatile int currentInvocationIndex = 0;

    // Do all updates to the current set from a single worker thread
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private long recordingIntervalStartedAtMillis = System.currentTimeMillis();

    private InvocationRegistry() {
        this.invocations = new Set[]{new HashSet<String>(), new HashSet<String>()};
        startWorker();
    }

    void startWorker() {
        if (!isNullRegistry()) {
            Thread worker = CodekvastThreadFactory.builder().name("registry").build().newThread(new InvocationsAdder());
            worker.start();
        }
    }

    public boolean isNullRegistry() {
        return false;
    }

    /**
     * Should be called before handing over to the AspectJ load-time weaver, or else nothing will be registered.
     *
     * @param config The collector configuration. May be null, in which case the registry is disabled.
     */
    public static void initialize(CollectorConfig config) {
        if (config == null) {
            instance = new NullInvocationRegistry();
            return;
        }

        InvocationRegistry.instance = new InvocationRegistry();
    }

    /**
     * Record that this method signature was invoked in the current recording interval.
     * <p>
     * Thread-safe.
     *
     * @param signature The captured method invocation signature.
     */
    public void registerMethodInvocation(Signature signature) {
        String sig = SignatureUtils.signatureToString(signature, false);

        // HashSet.contains() is thread-safe, so test first before deciding to add, but do the actual update from
        // a background worker thread.
        if (!invocations[currentInvocationIndex].contains(sig)) {
            queue.add(sig);
        }
    }

    public void publishInvocationData(@NonNull InvocationDataPublisher publisher) throws CodekvastPublishingException {
        long oldRecordingIntervalStartedAtMillis = recordingIntervalStartedAtMillis;
        int oldIndex = currentInvocationIndex;

        toggleInvocationsIndex();

        try {
            publisher.publishInvocationData(oldRecordingIntervalStartedAtMillis, invocations[oldIndex]);
        } finally {
            invocations[oldIndex].clear();
        }
    }

    private void toggleInvocationsIndex() {
        recordingIntervalStartedAtMillis = System.currentTimeMillis();
        currentInvocationIndex = currentInvocationIndex == 0 ? 1 : 0;
    }

    private class InvocationsAdder implements Runnable {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            while (true) {
                try {
                    invocations[currentInvocationIndex].add(queue.take());
                } catch (InterruptedException e) {
                    log.debug("Interrupted");
                    return;
                }
            }
        }
    }

    private static class NullInvocationRegistry extends InvocationRegistry {
        private NullInvocationRegistry() {
            super();
        }

        @Override
        public void registerMethodInvocation(Signature signature) {
            // No operation
        }

        @Override
        public boolean isNullRegistry() {
            return true;
        }

    }

}
