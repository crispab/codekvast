package se.crisp.codekvast.server.codekvast_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastProperties;

import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
public class WebController extends AbstractThymeleafController {

    @Inject
    private CodekvastProperties codekvastProperties;

    @RequestMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @RequestMapping({"/login"})
    public String login(Model model) {
        model.addAttribute("multiTenant", codekvastProperties.isMultiTenant());
        if (!codekvastProperties.isMultiTenant()) {
            model.addAttribute("username", "guest");
            model.addAttribute("password", "0000");
        }
        return "login";
    }

}
