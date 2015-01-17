package se.crisp.codekvast.agent.main;


import com.google.common.base.Preconditions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.crisp.codekvast.agent.config.AgentConfig;
import se.crisp.codekvast.agent.config.Sysprop;
import se.crisp.codekvast.agent.main.spring.AgentConfigPropertySource;
import se.crisp.codekvast.agent.util.FileUtils;
import se.crisp.codekvast.server.agent.ServerDelegateConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * The Spring Boot main program of the codekvast-agent.
 *
 * @author Olle Hallin
 */
@SpringBootApplication
@ComponentScan("se.crisp.codekvast")
@EnableScheduling
public class CodekvastAgentApplication {

    public static void main(String[] args) throws IOException, URISyntaxException {
        SpringApplication application = new SpringApplication(CodekvastAgentApplication.class);
        application.setDefaultProperties(getDefaultProperties());
        application.run(args);
    }
    private static Properties getDefaultProperties() {
        Properties result = new Properties();
        result.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
        return result;
    }

    /**
     * Make the AgentConfig object usable in SpringEL expressions with codekvast. as prefix...
     *
     * @param environment The configurable environment to modify.
     * @return The AgentConfig object that was appended to the environment's property sources.
     */
    @Bean
    public AgentConfig agentConfig(ConfigurableEnvironment environment) throws URISyntaxException {
        URL resource = FileUtils.safeGetURL(System.getProperty(Sysprop.AGENT_CONFIGURATION.toString()));
        if (resource == null) {
            resource = getClass().getResource("/codekvast-agent.conf");
        }
        Preconditions.checkNotNull(resource, "Cannot find classpath:/codekvast-agent.conf");
        AgentConfig agentConfig = AgentConfig.parseAgentConfigFile(resource.toURI());
        environment.getPropertySources().addLast(new AgentConfigPropertySource(agentConfig));
        return agentConfig;
    }

    /**
     * Converts an AgentConfig to a ServerDelegateConfig
     *
     * @param agentConfig The agent configuration object.
     * @return A server delegate config object.
     */
    @Bean
    public ServerDelegateConfig serverDelegateConfig(AgentConfig agentConfig) {
        return ServerDelegateConfig.builder()
                                   .environment(agentConfig.getEnvironment())
                                   .serverUri(agentConfig.getServerUri())
                                   .apiAccessID(agentConfig.getApiAccessID())
                                   .apiAccessSecret(agentConfig.getApiAccessSecret())
                                   .build();
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public ComputerID computerID() {
        return ComputerID.compute();
    }
}
