package se.crisp.codekvast.server.codekvast_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
public class WebController extends AbstractThymeleafController {

    @Inject
    private CodekvastSettings codekvastSettings;

    @RequestMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @RequestMapping({"/login"})
    public String login(Model model) {
        model.addAttribute("multiTenant", codekvastSettings.isMultiTenant());
        if (!codekvastSettings.isMultiTenant()) {
            model.addAttribute("username", "guest");
            model.addAttribute("password", "0000");
        }
        return "login";
    }

}
