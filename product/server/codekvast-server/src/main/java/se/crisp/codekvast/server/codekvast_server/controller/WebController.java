package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A Spring MVC Controller that handles traditional HTTP requests from the Codekvast web UI.
 *
 *
 * @author Olle Hallin
 */
@Controller
@Slf4j
public class WebController {

    @RequestMapping({"/", "/index"})
    public String index(ModelMap model) {
        return "index";
    }
}
