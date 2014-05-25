package duck.spike.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Olle Hallin
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class DuckServer {
    public static void main(String[] args) {
        SpringApplication.run(DuckServer.class, args);
    }
}
