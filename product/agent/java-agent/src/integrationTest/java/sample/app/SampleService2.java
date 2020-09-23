package sample.app;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

@Service
@Log
public class SampleService2 {

  public void doSomething(int p1) {
    logger.info("Doing something " + p1);
  }
}
