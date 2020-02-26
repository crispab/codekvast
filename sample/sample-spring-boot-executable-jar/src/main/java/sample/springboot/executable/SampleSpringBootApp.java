package sample.springboot.executable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class SampleSpringBootApp {

  public static void main(String[] args) {
    checkIfLoadedBySpringLoader();
    SpringApplication.run(SampleSpringBootApp.class, args);
  }

  private static void checkIfLoadedBySpringLoader() {
    try {
      Class.forName("org.springframework.boot.loader.JarLauncher");
      log.info("Loaded by Spring Boot Loader");
    } catch (ClassNotFoundException e) {
      log.info("Not loaded by Spring Boot Loader");
    }
  }
}
