package se.crisp.codekvast.support.web.config;

import com.planetj.servlet.filter.compression.CompressingFilter;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.servlet.Filter;
import java.io.File;

/**
 * Configures the embedded web server.
 *
 * If it finds a keystore in a well-known location, add an https connector on port 8443.
 * If the environment property "server.port" is something other than 8080 then the SSL port is chosen so that the difference is maintained.
 * Example: server.port=8090 results in the SSL port 8453.
 *
 * It also installs a response compressing filter.
 *
 * @author Olle Hallin
 */
@Configuration
public class WebServerConfig {

    public static final String KEYSTORE_PATH = "/etc/ssl/certs/java/star.crisp.se.jks";

    @Bean
    public EmbeddedServletContainerFactory servletContainer(Environment environment) {
        Integer serverPort = environment.getProperty("server.port", Integer.class, 8080);
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

        File keystore = new File(KEYSTORE_PATH);
        if (keystore.canRead() && serverPort != 0) {
            // Don't configure an SSL port during tests.
            tomcat.addAdditionalTomcatConnectors(createSslConnector(keystore, computeSslPort(serverPort)));
        }
        return tomcat;
    }

    private int computeSslPort(int serverPort) {
        return 8443 + serverPort - 8080;
    }

    private Connector createSslConnector(File keystore, int port) {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setPort(port);

        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setSSLEnabled(true);
        protocol.setKeystoreFile(keystore.getAbsolutePath());
        protocol.setKeystorePass("4M2oTshcj");
        protocol.setKeyAlias("crisp");
        return connector;
    }

    @Bean
    public Filter compressingFilter() {
        return new CompressingFilter();
    }

}
