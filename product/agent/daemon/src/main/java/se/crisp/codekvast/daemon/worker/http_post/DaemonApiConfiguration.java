package se.crisp.codekvast.daemon.worker.http_post;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.crisp.codekvast.daemon.beans.DaemonConfig;
import se.crisp.codekvast.server.daemon_api.DaemonApiConfig;

import static se.crisp.codekvast.daemon.DaemonConstants.HTTP_POST_PROFILE;

/**
 * Factory for Spring beans only used in the HTTP POST profile
 *
 * @author olle.hallin@crisp.se
 */
@Configuration
@Profile(HTTP_POST_PROFILE)
public class DaemonApiConfiguration {
    /**
     * Converts an DaemonConfig to a DaemonApiConfig
     *
     * @param daemonConfig The daemon configuration object.
     * @return A server delegate config object.
     */
    @Bean
    public DaemonApiConfig serverDelegateConfig(DaemonConfig daemonConfig) {
        return DaemonApiConfig.builder()
                              .serverUri(daemonConfig.getServerUri())
                              .apiAccessID(daemonConfig.getApiAccessID())
                              .apiAccessSecret(daemonConfig.getApiAccessSecret())
                              .build();
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

}
