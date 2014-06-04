package se.crisp.duck.server;

import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Olle Hallin
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class DuckServer {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DuckServer.class);
        application.setDefaultProperties(loadProperties("default.properties"));
        application.run(args);
    }

    @SneakyThrows(IOException.class)
    private static Properties loadProperties(String resource) {
        Properties result = new Properties();
        result.load(DuckServer.class.getClassLoader().getResourceAsStream(resource));
        result.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
        return result;
    }
}
