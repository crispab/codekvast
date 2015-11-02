package se.crisp.codekvast.daemon;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.server.daemon_api.DaemonApiConfig;
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
                                   "classpath:/" + DaemonConstants.DAEMON_CONFIG_FILE);
        SpringApplication application = new SpringApplication(CodekvastDaemon.class);
        application.run(args);
    }
    /**
     * Converts an DaemonConfig to a DaemonApiConfig
     *
     * @param daemonConfig The daemon configuration object.
     * @return A server delegate config object.
     */
    @Bean
    public DaemonApiConfig serverDelegateConfig(DaemonConfig daemonConfig) {
        return DaemonApiConfig.builder()
                                   .serverUri(daemonConfig.getServerUri())
                                   .apiAccessID(daemonConfig.getApiAccessID())
                                   .apiAccessSecret(daemonConfig.getApiAccessSecret())
                                   .build();
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

}
