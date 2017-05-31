package io.codekvast.sample.codekvastspringheroku.button.one;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ButtonOneService {

    private static final Logger logger = LoggerFactory.getLogger(ButtonOneService.class);

    public void doSomething() {
        logger.info("Doing something 1");
    }
}
