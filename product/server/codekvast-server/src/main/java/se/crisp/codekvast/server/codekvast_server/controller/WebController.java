package se.crisp.codekvast.server.codekvast_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Olle Hallin
 */
@Controller
public class WebController extends AbstractThymeleafController {

    @RequestMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @RequestMapping({"/login"})
    public String login() {
        return "login";
    }

}
