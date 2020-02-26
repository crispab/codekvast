package sample.springboot.executable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** @author olle.hallin@crisp.se */
@Component
@Profile("foo")
@Slf4j
public class FooService {

  @PostConstruct
  public void postConstruct() {
    log.info("Hello from FooService");
  }
}
