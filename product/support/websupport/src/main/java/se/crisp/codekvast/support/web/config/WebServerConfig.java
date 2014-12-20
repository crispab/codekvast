package se.crisp.codekvast.support.web.config;

import com.planetj.servlet.filter.compression.CompressingFilter;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.io.File;

/**
 * Configures the embedded web server.
 *
 * @author Olle Hallin
 */
@Configuration
public class WebServerConfig {

    /**
     * If it finds a keystore in a well-known location, add an https connector on port 8443 in addition to the standard http connector on
     * port 8080.
     */
    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

        File keystore = new File("/etc/ssl/certs/java/star.crisp.se.jks");
        if (keystore.canRead()) {
            tomcat.addAdditionalTomcatConnectors(createSslConnector(keystore, 8443));
        }
        return tomcat;
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
