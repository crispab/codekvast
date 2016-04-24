/**
 * Copyright (c) 2015-2016 Crisp AB
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
package se.crisp.codekvast.agent.daemon.worker.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import se.crisp.codekvast.agent.daemon.appversion.AppVersionResolver;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.agent.daemon.beans.JvmState;
import se.crisp.codekvast.agent.daemon.codebase.CodeBase;
import se.crisp.codekvast.agent.daemon.codebase.CodeBaseScanner;
import se.crisp.codekvast.agent.daemon.util.LogUtil;
import se.crisp.codekvast.agent.daemon.worker.CollectorDataProcessor;
import se.crisp.codekvast.agent.daemon.worker.DataProcessingException;
import se.crisp.codekvast.agent.lib.model.Invocation;
import se.crisp.codekvast.agent.lib.model.Jvm;
import se.crisp.codekvast.agent.lib.model.v1.JvmData;
import se.crisp.codekvast.agent.lib.model.v1.SignatureStatus;
import se.crisp.codekvast.agent.lib.util.ComputerID;
import se.crisp.codekvast.agent.lib.util.FileUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.Instant.now;
import static se.crisp.codekvast.agent.lib.model.v1.SignatureStatus.*;

/**
 * Common behaviour for all data processors.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractCollectorDataProcessorImpl implements CollectorDataProcessor {
    protected final DaemonConfig daemonConfig;
    private final AppVersionResolver appVersionResolver;
    private final CodeBaseScanner codeBaseScanner;

    private final String daemonComputerId = ComputerID.compute().toString();
    private final String daemonHostName = getHostName();

    @Getter
    private Instant lastCollectorDataProcessedAt = Instant.MIN;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processCollectorData(JvmState jvmState, CodeBase codeBase) throws DataProcessingException {
        appVersionResolver.resolveAppVersion(jvmState);
        processJvmData(jvmState);
        processCodeBase(jvmState, codeBase);
        processInvocationsData(jvmState);
    }

    private void processJvmData(JvmState jvmState) throws DataProcessingException {
        if (jvmState.getJvmDumpedAt().isAfter(jvmState.getJvmDataProcessedAt())) {
            try {
                doProcessJvmData(jvmState);
                jvmState.setJvmDataProcessedAt(jvmState.getJvmDumpedAt());

                recordLastCollectorDataProcessed("JVM data");
            } catch (Exception e) {
                LogUtil.logException(log, "Cannot process JVM data", e);
            }
        }
    }

    private void processCodeBase(JvmState jvmState, CodeBase codeBase) {
        if (!codeBase.equals(jvmState.getCodeBase())) {
            if (jvmState.getCodeBase() == null) {
                log.debug("Codebase has not yet been processed");
            } else {
                appVersionResolver.resolveAppVersion(jvmState);
                log.info("Codebase has changed, it will now be re-scanned and processed");
            }

            codeBaseScanner.scanSignatures(codeBase);

            try {
                doProcessCodebase(jvmState, codeBase);
                jvmState.setCodeBase(codeBase);

                recordLastCollectorDataProcessed("Codebase");
            } catch (Exception e) {
                LogUtil.logException(log, "Cannot process code base", e);
            }
        }
    }

    private void processInvocationsData(JvmState jvmState) throws DataProcessingException {
        List<Invocation> invocations = FileUtils.consumeAllInvocationDataFiles(jvmState.getInvocationsFile());
        if (jvmState.getCodeBase() != null && !invocations.isEmpty()) {
            doProcessInvocations(jvmState, invocations);
            doProcessUnprocessedInvocations(jvmState);

            recordLastCollectorDataProcessed("Invocation data");
        }
    }

    private void recordLastCollectorDataProcessed(String what) {
        lastCollectorDataProcessedAt = now();
        log.debug("{} processed at {}", what, lastCollectorDataProcessedAt);
    }

    protected abstract void doProcessJvmData(JvmState jvmState) throws DataProcessingException;

    protected abstract void doProcessCodebase(JvmState jvmState, CodeBase codeBase) throws DataProcessingException;

    protected abstract void doProcessUnprocessedInvocations(JvmState jvmState) throws DataProcessingException;

    protected abstract void doStoreNormalizedSignature(JvmState jvmState, String normalizedSignature,
                                                       long invokedAtMillis, SignatureStatus status);

    private void doProcessInvocations(JvmState jvmState, List<Invocation> invocations) {
        CodeBase codeBase = jvmState.getCodeBase();

        int recognized = 0;
        int unrecognized = 0;
        int ignored = 0;
        int overridden = 0;

        for (Invocation invocation : invocations) {
            String rawSignature = invocation.getSignature();
            String normalizedSignature = codeBase.normalizeSignature(rawSignature);
            String baseSignature = codeBase.getBaseSignature(normalizedSignature);

            SignatureStatus status = null;
            if (normalizedSignature == null) {
                ignored += 1;
            } else if (codeBase.hasSignature(normalizedSignature)) {
                recognized += 1;
                status = EXACT_MATCH;
            } else if (baseSignature != null) {
                overridden += 1;
                status = FOUND_IN_PARENT_CLASS;
                log.debug("Signature '{}' is replaced by '{}'", normalizedSignature, baseSignature);
                normalizedSignature = baseSignature;
            } else if (normalizedSignature.equals(rawSignature)) {
                unrecognized += 1;
                status = NOT_FOUND_IN_CODE_BASE;
                log.debug("Unrecognized signature: '{}'", normalizedSignature);
            } else {
                unrecognized += 1;
                status = NOT_FOUND_IN_CODE_BASE;
                log.debug("Unrecognized signature: '{}' (was '{}')", normalizedSignature, rawSignature);
            }

            if (normalizedSignature != null) {
                doStoreNormalizedSignature(jvmState, normalizedSignature, invocation.getInvokedAtMillis(), status);
            }
        }

        FileUtils.deleteAllConsumedInvocationDataFiles(jvmState.getInvocationsFile());

        // For debugging...
        codeBase.writeSignaturesToDisk();

        if (unrecognized > 0) {
            log.warn("{} recognized, {} overridden, {} unrecognized and {} ignored method invocations applied", recognized, overridden,
                     unrecognized, ignored);
        } else {
            log.debug("{} signature invocations applied ({} overridden, {} ignored)", recognized, overridden, ignored);
        }
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Cannot get name of localhost");
            return "-- unknown --";
        }
    }

    protected JvmData createJvmData(JvmState jvmState) {
        Jvm jvm = jvmState.getJvm();

        return JvmData.builder()
                      .appName(jvm.getCollectorConfig().getAppName())
                      .appVersion(jvmState.getAppVersion())
                      .collectorComputerId(jvm.getComputerId())
                      .collectorHostName(jvm.getHostName())
                      .collectorResolutionSeconds(jvm.getCollectorConfig().getCollectorResolutionSeconds())
                      .collectorVcsId(jvm.getCollectorVcsId())
                      .collectorVersion(jvm.getCollectorVersion())
                      .daemonComputerId(daemonComputerId)
                      .daemonHostName(daemonHostName)
                      .daemonVcsId(daemonConfig.getDaemonVcsId())
                      .daemonVersion(daemonConfig.getDaemonVersion())
                      .dumpedAtMillis(jvm.getDumpedAtMillis())
                      .environment(daemonConfig.getEnvironment())
                      .excludePackages(jvm.getCollectorConfig().getNormalizedExcludePackages().stream().collect(Collectors.joining(", ")))
                      .jvmUuid(jvm.getJvmUuid())
                      .methodVisibility(jvm.getCollectorConfig().getMethodAnalyzer().toString())
                      .packages(jvm.getCollectorConfig().getNormalizedPackages().stream().collect(Collectors.joining(", ")))
                      .startedAtMillis(jvm.getStartedAtMillis())
                      .tags(jvm.getCollectorConfig().getTags())
                      .build();
    }
}
