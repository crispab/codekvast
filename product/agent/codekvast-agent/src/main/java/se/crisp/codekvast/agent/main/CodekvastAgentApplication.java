package se.crisp.codekvast.agent.main;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.crisp.codekvast.server.agent_api.AgentApiConfig;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * The Spring Boot main program of the codekvast-agent.
 *
 * @author olle.hallin@crisp.se
 */
@SpringBootApplication
@ComponentScan("se.crisp.codekvast")
@EnableScheduling
public class CodekvastAgentApplication {

    public static void main(String[] args) throws IOException, URISyntaxException {
        System.setProperty("spring.config.location",
                           "classpath:/application.properties," +
                                   "classpath:/default.properties," +
                                   "classpath:/codekvast-agent.properties");
        SpringApplication application = new SpringApplication(CodekvastAgentApplication.class);
        application.run(args);
    }
    /**
     * Converts an AgentConfig to a AgentApiConfig
     *
     * @param agentConfig The agent configuration object.
     * @return A server delegate config object.
     */
    @Bean
    public AgentApiConfig serverDelegateConfig(AgentConfig agentConfig) {
        return AgentApiConfig.builder()
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
