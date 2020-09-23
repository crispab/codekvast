package sample.app;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/** @author olle.hallin@crisp.se */
@SuppressWarnings("ALL")
@Log
@SpringBootApplication
@EnableAspectJAutoProxy
@RequiredArgsConstructor
public class SampleApp {
  private final int dummy = 17;
  private final SampleService1 sampleService1;

  public int add(int p1, int p2) {
    return privateAdd(p1, p2);
  }

  private int privateAdd(int p1, int p2) {
    return p1 + p2;
  }

  @PostConstruct
  public void postConstruct() {
    logger.info("2+2=" + add(2, 2));
    sampleService1.doSomething(1);
  }

  public static void main(String[] args) throws InterruptedException {
    logger.info(
        String.format(
            "%s starts on Java %s",
            SampleApp.class.getSimpleName(), System.getProperty("java.version")));
    SpringApplication.run(SampleApp.class, args);
    logger.info("Exit");
  }
}
