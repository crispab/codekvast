package se.crisp.duck.server.agent.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * This is a Spring @Configuration module
 *
 * @author Olle Hallin
 */
@Configuration
public class ServerDelegateModule {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
