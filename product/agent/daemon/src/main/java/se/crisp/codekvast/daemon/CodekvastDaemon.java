package se.crisp.codekvast.daemon;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.crisp.codekvast.daemon.beans.AgentConfig;
import se.crisp.codekvast.server.daemon_api.AgentApiConfig;
import se.crisp.codekvast.support.common.LoggingConfig;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * The Spring Boot main program of the codekvast-daemon.
 *
 * @author olle.hallin@crisp.se
 */
@SpringBootApplication
@ComponentScan("se.crisp.codekvast")
@EnableScheduling
public class CodekvastDaemon {

    public static void main(String[] args) throws IOException, URISyntaxException {
        LoggingConfig.configure(CodekvastDaemon.class, "codekvast-daemon");
        System.setProperty("spring.config.location",
                           "classpath:/application.properties," +
                                   "classpath:/default.properties," +
                                   "classpath:/codekvast-daemon.properties");
        SpringApplication application = new SpringApplication(CodekvastDaemon.class);
        application.run(args);
    }
    /**
     * Converts an AgentConfig to a AgentApiConfig
     *
     * @param agentConfig The daemon configuration object.
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
