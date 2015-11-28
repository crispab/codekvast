package se.crisp.codekvast.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import se.crisp.codekvast.support.common.LoggingConfig;

import java.io.IOException;
import java.util.Properties;

/**
 * The Spring Boot main for codekvast-warehouse,
 *
 * @author olle.hallin@crisp.se
 */
@SpringBootApplication
@ComponentScan({"se.crisp.codekvast.warehouse", "se.crisp.codekvast.support"})
@EnableScheduling
public class CodekvastWarehouseApplication {

    public static void main(String[] args) throws IOException {
        LoggingConfig.configure(CodekvastWarehouseApplication.class, "codekvast-warehouse");
        System.setProperty("spring.config.location",
                           "classpath:/application.properties," +
                                   "classpath:/default.properties," +
                                   "classpath:/codekvast-warehouse.properties");
        SpringApplication application = new SpringApplication(CodekvastWarehouseApplication.class);
        application.setDefaultProperties(loadDefaultProperties());
        application.run(args);
    }

    private static Properties loadDefaultProperties() {
        Properties result = new Properties();
        result.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
        return result;
    }

}
