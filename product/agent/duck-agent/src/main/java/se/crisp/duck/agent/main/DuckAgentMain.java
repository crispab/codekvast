package se.crisp.duck.agent.main;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import se.crisp.duck.agent.util.AgentConfig;
import se.crisp.duck.server.agent.AgentDelegate;
import se.crisp.duck.server.agent.impl.AgentDelegateImpl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * @author Olle Hallin
 */
@Configuration
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan("se.crisp.duck")
public class DuckAgentMain {

    private static URI agentConfigLocation;

    public static void main(String[] args) throws IOException, URISyntaxException {
        DuckAgentMain.agentConfigLocation = getAgentConfigLocation(args);

        SpringApplication application = new SpringApplication(DuckAgentMain.class);
        application.setDefaultProperties(loadDefaultProperties());
        application.run(args);
    }

    private static Properties loadDefaultProperties() {
        Properties result = new Properties();
        result.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
        result.setProperty("spring.config.location", agentConfigLocation.toString());
        result.setProperty("spring.config.name", "duck.properties");
        return result;
    }

    public static URI getAgentConfigLocation(String[] args) throws URISyntaxException {
        return args == null || args.length < 1 ? new URI("classpath:/duck.properties") : new File(args[0]).toURI();
    }

    @Bean
    public static AgentConfig agentConfig(ConfigurableEnvironment environment) {
        AgentConfig agentConfig = AgentConfig.parseConfigFile(agentConfigLocation);

        // Make the AgentConfig object usable in SpringEL expressions with duck. as prefix...
        environment.getPropertySources().addLast(new PropertySource("agentConfig", agentConfig) {
            @Override
            public Object getProperty(String name) {
                String prefix = "duck.";
                if (name.startsWith(prefix)) {
                    try {
                        Field field = null;
                        field = AgentConfig.class.getDeclaredField(name.substring(prefix.length()));
                        if (!Modifier.isStatic(field.getModifiers())) {
                            field.setAccessible(true);
                            return field.get(getSource());
                        }
                    } catch (NoSuchFieldException ignore) {
                    } catch (IllegalAccessException ignore) {
                    }
                }
                return null;
            }
        });
        return agentConfig;
    }

    @Bean
    public static AgentDelegate agentDelegate(AgentConfig agentConfig) {
        return new AgentDelegateImpl(agentConfig.getServerUri());
    }

}
