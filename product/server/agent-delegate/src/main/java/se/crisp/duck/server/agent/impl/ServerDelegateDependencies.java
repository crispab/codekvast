package se.crisp.duck.server.agent.impl;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import se.crisp.duck.server.agent.ServerDelegateConfig;

import javax.inject.Inject;

/**
 * This is a Spring @Configuration that creates the dependencies to inject into ServerDelegateImpl.
 *
 * @author Olle Hallin
 */
@Configuration
public class ServerDelegateDependencies {

    @Inject
    private ServerDelegateConfig config;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(createBasicAuthHttpClient(config.getApiUsername(), config.getApiPassword()));
    }

    private HttpComponentsClientHttpRequestFactory createBasicAuthHttpClient(final String username, final String password) {
        CredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return new HttpComponentsClientHttpRequestFactory(HttpClients.custom().setDefaultCredentialsProvider(cp).build());
    }
}
