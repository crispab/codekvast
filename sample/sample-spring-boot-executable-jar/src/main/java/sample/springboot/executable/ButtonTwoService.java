package sample.springboot.executable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ButtonTwoService {

  @SuppressWarnings("unused")
  public void doSomething() {
    log.info("Doing something 2");
  }
}
