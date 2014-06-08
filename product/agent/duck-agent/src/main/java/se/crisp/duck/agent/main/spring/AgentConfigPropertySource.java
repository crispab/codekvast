package se.crisp.duck.agent.main.spring;

import org.springframework.core.env.PropertySource;
import se.crisp.duck.agent.util.AgentConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Make an AgentConfig usable in a Spring Environment, so that AgentConfig properties are injectable
 * as e.g., @Value("${duck.packagePrefix}")
 *
 * @author Olle Hallin
 */
public class AgentConfigPropertySource extends PropertySource<AgentConfig> {

    private final String prefix;

    public AgentConfigPropertySource(AgentConfig agentConfig, String prefix) {
        super("agentConfig", agentConfig);
        this.prefix = prefix;
    }

    @Override
    public Object getProperty(String name) {
        if (name.startsWith(prefix)) {
            try {
                Field field;
                field = AgentConfig.class.getDeclaredField(name.substring(prefix.length()));
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    return field.get(getSource());
                }
            } catch (NoSuchFieldException | IllegalAccessException ignore) {
            }
        }
        return null;
    }
}
