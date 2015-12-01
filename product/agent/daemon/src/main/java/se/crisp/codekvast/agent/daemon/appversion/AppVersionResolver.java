/**
 * Copyright (c) 2015 Crisp AB
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
package se.crisp.codekvast.agent.daemon.appversion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.daemon.beans.JvmState;
import se.crisp.codekvast.agent.lib.model.Jvm;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

@Component
@Slf4j
public class AppVersionResolver {

    private final Collection<AppVersionStrategy> appVersionStrategies = new ArrayList<AppVersionStrategy>();

    @Inject
    public AppVersionResolver(Collection<AppVersionStrategy> appVersionStrategies) {
        this.appVersionStrategies.addAll(appVersionStrategies);
    }

    static String resolveAppVersion(Collection<? extends AppVersionStrategy> appVersionStrategies,
                                    Collection<File> codeBases,
                                    String appVersion) {
        String version = appVersion.trim();
        String args[] = version.split("\\s+");

        for (AppVersionStrategy strategy : appVersionStrategies) {
            if (strategy.canHandle(args)) {
                long startedAt = System.currentTimeMillis();
                String resolvedVersion = strategy.resolveAppVersion(codeBases, args);
                log.debug("Resolved '{}' to '{}' in {} ms", version, resolvedVersion, System.currentTimeMillis() - startedAt);
                return resolvedVersion;
            }
        }
        log.debug("Cannot resolve appVersion '{}', using it verbatim", version);
        return version;
    }

    public void resolveAppVersion(JvmState jvmState) {
        String oldAppVersion = jvmState.getAppVersion();

        Jvm jvm = jvmState.getJvm();

        String newAppVersion = resolveAppVersion(appVersionStrategies, jvm.getCollectorConfig().getCodeBaseFiles(),
                                                 jvm.getCollectorConfig().getAppVersion());

        if (oldAppVersion == null) {
            log.info("{} has version '{}'", jvmState.getJvm().getCollectorConfig().getAppName(), newAppVersion);
        } else if (!newAppVersion.equals(oldAppVersion)) {
            log.info("The version of {} has changed from '{}' to '{}'", jvmState.getJvm().getCollectorConfig().getAppName(),
                     oldAppVersion, newAppVersion);
        }

        jvmState.setAppVersion(newAppVersion);
    }


}
