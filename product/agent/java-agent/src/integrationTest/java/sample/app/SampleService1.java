package sample.app;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log
public class SampleService1 {
  private final SampleService2 sampleService2;

  public void doSomething(int p1) {
    logger.info("Doing something " + p1);
    sampleService2.doSomething(p1 * 2);
  }
}
