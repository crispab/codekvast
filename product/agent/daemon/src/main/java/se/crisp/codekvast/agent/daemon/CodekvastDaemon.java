package se.crisp.codekvast.agent.daemon;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
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
}
