package se.crisp.duck.server.agent.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * This is a Spring @Configuration that creates the dependencies to inject into ServerDelegateImpl.
 *
 * @author Olle Hallin
 */
@Configuration
public class ServerDelegateDependencies {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
