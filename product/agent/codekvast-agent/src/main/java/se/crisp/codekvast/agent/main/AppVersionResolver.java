package se.crisp.codekvast.agent.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import se.crisp.codekvast.agent.appversion.AppVersionStrategy;
import se.crisp.codekvast.shared.model.Jvm;

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
