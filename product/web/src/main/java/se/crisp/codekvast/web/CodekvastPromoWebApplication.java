package se.crisp.codekvast.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Spring Boot main for codekvast-server,
 *
 * @author Olle Hallin
 */
@SpringBootApplication
@EnableScheduling
public class CodekvastPromoWebApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication application = new SpringApplication(CodekvastPromoWebApplication.class);
        application.setDefaultProperties(loadDefaultProperties());
        application.run(args);
    }

    private static Properties loadDefaultProperties() throws IOException {
        Properties result = new Properties();
        result.load(getInputStream("default.properties"));
        result.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
        return result;
    }

    private static InputStream getInputStream(String resource) throws IOException {
        InputStream result = CodekvastPromoWebApplication.class.getClassLoader().getResourceAsStream(resource);
        if (result == null) {
            result = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        }
        if (result == null) {
            throw new IOException("Cannot find " + resource);
        }
        return result;
    }
}
