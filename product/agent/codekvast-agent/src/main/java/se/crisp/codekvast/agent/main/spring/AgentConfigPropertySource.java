package se.crisp.codekvast.agent.main.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.PropertySource;
import se.crisp.codekvast.agent.config.AgentConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Make an AgentConfig usable in a Spring Environment, so that AgentConfig properties are injectable as e.g.,
 * <pre><code>@Value("${codekvast.packagePrefixes}")</code></pre>
 *
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public class AgentConfigPropertySource extends PropertySource<AgentConfig> {

    private final String prefix = "codekvast.";

    public AgentConfigPropertySource(AgentConfig agentConfig) {
        super("agentConfig", agentConfig);
    }

    @Override
    public Object getProperty(String name) {
        if (name.startsWith(prefix)) {
            String getterName = getGetterName(name);
            String fullMethodName = AgentConfig.class.getName() + "." + getterName + "()";
            log.debug("Attempting to invoke {}", fullMethodName);
            try {
                Method method = AgentConfig.class.getMethod(getterName);
                return method.invoke(getSource());
            } catch (NoSuchMethodException e) {
                log.debug("No such method: " + fullMethodName);
            } catch (InvocationTargetException | IllegalAccessException e) {
                log.warn("Cannot invoke " + fullMethodName, e);
            }
        }
        return null;
    }

    private String getGetterName(String prefixedName) {
        String name = prefixedName.substring(prefix.length());
        return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
