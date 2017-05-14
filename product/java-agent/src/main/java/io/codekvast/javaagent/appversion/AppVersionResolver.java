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
package io.codekvast.javaagent.appversion;

import io.codekvast.javaagent.config.AgentConfig;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.Collection;

@Log
public class AppVersionResolver {

    private final Collection<AppVersionStrategy> appVersionStrategies = new ArrayList<>();
    private final AgentConfig config;

    public AppVersionResolver(AgentConfig config) {
        this.config = config;

        this.appVersionStrategies.add(new LiteralAppVersionStrategy());
        this.appVersionStrategies.add(new ManifestAppVersionStrategy());
        this.appVersionStrategies.add(new FilenameAppVersionStrategy());
    }

    public String resolveAppVersion() {
        String version = config.getAppVersion().trim();
        String args[] = version.split("\\s+");

        for (AppVersionStrategy strategy : appVersionStrategies) {
            if (strategy.canHandle(args)) {
                String resolvedVersion = strategy.resolveAppVersion(config.getCodeBaseFiles(), args);
                log.fine(String.format("Resolved appVersion '%s' to '%s'", version, resolvedVersion));
                return resolvedVersion;
            }
        }

        log.fine(String.format("Cannot resolve appVersion '%s', using it as-is", version));
        return version;
    }

}
