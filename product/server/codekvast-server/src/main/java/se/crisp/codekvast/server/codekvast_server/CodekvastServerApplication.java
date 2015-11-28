package se.crisp.codekvast.server.codekvast_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import se.crisp.codekvast.support.common.LoggingConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * The Spring Boot main for codekvast-server,
 *
 * @author olle.hallin@crisp.se
 */
@SpringBootApplication
@ComponentScan({"se.crisp.codekvast.server.codekvast_server", "se.crisp.codekvast.support"})
public class CodekvastServerApplication {

    public static void main(String[] args) throws IOException {
        LoggingConfig.configure(CodekvastServerApplication.class, "codekvast-server");
        System.setProperty("spring.config.location",
                           "classpath:/application.properties," +
                                   "classpath:/default.properties," +
                                   "classpath:/codekvast-server.properties");
        SpringApplication application = new SpringApplication(CodekvastServerApplication.class);
        application.setDefaultProperties(loadDefaultProperties());
        application.run(args);
    }

    private static Properties loadDefaultProperties() {
        Properties result = new Properties();
        result.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
        return result;
    }

}
