package se.crisp.codekvast.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    @Value("${spring.thymeleaf.cache}")
    private Boolean thymeleafCache;

    @RequestMapping({"/", "/page/**"})
    public String index(ModelMap model) {
        model.put("thymeleafCache", thymeleafCache);
        return "index";
    }
}
