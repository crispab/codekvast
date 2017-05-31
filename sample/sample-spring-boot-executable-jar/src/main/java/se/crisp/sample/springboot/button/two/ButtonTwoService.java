package io.codekvast.sample.codekvastspringheroku.button.two;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ButtonTwoService {

    private static final Logger logger = LoggerFactory.getLogger(ButtonTwoService.class);

    public void doSomething() {
        logger.info("Doing something 2");
    }
}
