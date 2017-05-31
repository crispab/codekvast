package sample.springbootrepackaged;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sample.lib.used.Bar1;

@Service
@Slf4j
public class ButtonOneService {

    @SuppressWarnings("unused")
    void doSomething() {
        log.info("Doing something 1: {}", new Bar1().declaredOnBar());
    }
}
