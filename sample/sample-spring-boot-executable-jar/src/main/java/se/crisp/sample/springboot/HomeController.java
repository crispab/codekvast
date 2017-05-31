package io.codekvast.sample.codekvastspringheroku;

import io.codekvast.sample.codekvastspringheroku.button.one.ButtonOneService;
import io.codekvast.sample.codekvastspringheroku.button.two.ButtonTwoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@Controller
@RequestMapping(method = GET)
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final ButtonOneService buttonOneService;
    private final ButtonTwoService buttonTwoService;

    @Autowired
    public HomeController(ButtonOneService buttonOneService,
                          ButtonTwoService buttonTwoService) {
        this.buttonOneService = buttonOneService;
        this.buttonTwoService = buttonTwoService;
    }

    @RequestMapping(value = "/", method = GET)
    String home()  {
        logger.info("Welcome home.");
        return "/home.html";
    }

    @RequestMapping("/button1")
    String buttonOne() {
        buttonOneService.doSomething();
        return "/buttonOne.html";
    }

    @RequestMapping("/button2")
    String buttonTwo() {
        buttonTwoService.doSomething();
        return "/buttonTwo.html";
    }
}

