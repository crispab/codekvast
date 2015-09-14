package sample.app.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import se.crisp.codekvast.agent.collector.AbstractMethodExecutionAspect;
import se.crisp.codekvast.agent.collector.CodekvastCollector;
import se.crisp.codekvast.agent.config.CollectorConfig;
import se.crisp.codekvast.agent.io.FileSystemDataDumper;
import se.crisp.codekvast.agent.util.ConfigUtils;

import java.io.File;

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
        CodekvastCollector.initialize(config, new FileSystemDataDumper(config, System.err));
    }
}
