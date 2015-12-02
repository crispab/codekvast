package se.crisp.codekvast.agent.daemon.worker.http_post;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.crisp.codekvast.agent.daemon.beans.DaemonConfig;
import se.crisp.codekvast.server.daemon_api.DaemonApiConfig;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Factory for Spring beans only used in the HTTP POST profile
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
@Profile("httpPost")
public class HttpPostBeanFactory {
    /**
     * Converts an DaemonConfig to a DaemonApiConfig
     *
     * @param daemonConfig The daemon configuration object.
     * @return A server delegate config object.
     */
    @Bean
    public DaemonApiConfig serverDelegateConfig(DaemonConfig daemonConfig) throws URISyntaxException {
        return DaemonApiConfig.builder()
                              .serverUri(new URI("daemonConfig.getServerUri()"))
                              .apiAccessID("daemonConfig.getApiAccessID()")
                              .apiAccessSecret("daemonConfig.getApiAccessSecret()")
                              .build();
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

}
