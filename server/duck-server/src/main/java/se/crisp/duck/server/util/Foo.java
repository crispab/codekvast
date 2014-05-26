package se.crisp.duck.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author Olle Hallin
 */
@Component
@Slf4j
public class Foo {

    @Value("${h2database.path}")
    private String dbPath;

    @PostConstruct
    public void postConstruct() {
        log.info("h2database.path={}", dbPath);
    }
}
