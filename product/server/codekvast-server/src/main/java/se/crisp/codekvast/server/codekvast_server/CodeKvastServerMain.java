package se.crisp.codekvast.server.codekvast_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The Spring Boot main for codekvast-server,
 *
 * @author Olle Hallin
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class CodeKvastServerMain {

    public static void main(String[] args) throws IOException {
        SpringApplication application = new SpringApplication(CodeKvastServerMain.class);
        application.setDefaultProperties(loadProperties("default.properties"));
        application.run(args);
    }

    private static Properties loadProperties(String resource) throws IOException {
        Properties result = new Properties();
        result.load(getInputStream(resource));
        result.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
        return result;
    }

    private static InputStream getInputStream(String resource) throws IOException {
        InputStream result = CodeKvastServerMain.class.getClassLoader().getResourceAsStream(resource);
        if (result == null) {
            result = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        }
        if (result == null) {
            throw new IOException("Cannot find " + resource);
        }
        return result;
    }
}
