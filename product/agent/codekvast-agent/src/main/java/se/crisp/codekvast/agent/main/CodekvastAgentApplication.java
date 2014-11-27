package se.crisp.codekvast.agent.main;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.crisp.codekvast.agent.config.AgentConfig;
import se.crisp.codekvast.agent.main.spring.AgentConfigPropertySource;
import se.crisp.codekvast.server.agent.ServerDelegateConfig;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * The Spring Boot main program of the codekvast-agent.
 *
 * @author Olle Hallin
 */
@Configuration
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan("se.crisp.codekvast")
public class CodekvastAgentApplication {

    private static AgentConfig agentConfig;

    public static void main(String[] args) throws IOException, URISyntaxException {
        SpringApplication application = new SpringApplication(CodekvastAgentApplication.class);
        application.setDefaultProperties(getDefaultProperties());
        application.run(args);
    }

    private static URI getAgentConfigLocation(String[] args) throws URISyntaxException {
        // TODO: Look for file:/etc/codekvast.conf if not in args
        return args == null || args.length < 1 ? new URI("classpath:/codekvast.conf") : new File(args[0]).toURI();
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
    public AgentConfig agentConfig(ConfigurableEnvironment environment) {
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

}
