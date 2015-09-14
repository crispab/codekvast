package sample.app.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import se.crisp.codekvast.agent.collector.AbstractMethodExecutionAspect;
import se.crisp.codekvast.agent.collector.CodekvastCollector;
import se.crisp.codekvast.shared.config.CollectorConfig;
import se.crisp.codekvast.shared.io.FileSystemInvocationDataDumper;
import se.crisp.codekvast.shared.util.ConfigUtils;

import java.io.File;

/**
 * Example that shows how to both make methodExecution() concrete and initializing the CodekvastCollector.
 */
@Aspect
public class MethodExecutionAspect extends AbstractMethodExecutionAspect {

    @Pointcut("execution(* *..*(..)) && within(sample..*)")
    @Override
    public void methodExecution() {
    }

    static {
        CollectorConfig config = CollectorConfig
                .builder()
                .appName(ConfigUtils.expandVariables(null, "$APP_NAME"))
                .appVersion("from static aspect")
                .collectorResolutionSeconds(5)
                .methodVisibility("public")
                .codeBase("$APP_HOME/lib")
                .packagePrefixes("se.")
                .verbose(true)
                .dataPath(new File("/tmp/codekvast"))
                .tags("")
                .build();
        CodekvastCollector.initialize(config, new FileSystemInvocationDataDumper(config, System.err));
    }
}
