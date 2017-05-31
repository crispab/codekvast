package sample.springbootrepackaged;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ButtonOneService {

    @SuppressWarnings("unused")
    public void doSomething() {
        log.info("Doing something 1");
    }
}
