package se.crisp.sample.springboot.button.two;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ButtonTwoService {

    private static final Logger logger = LoggerFactory.getLogger(ButtonTwoService.class);

    @SuppressWarnings("unused")
    public void doSomething() {
        logger.info("Doing something 2");
    }
}
