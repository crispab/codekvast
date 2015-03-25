package se.crisp.codekvast.server.codekvast_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import se.crisp.codekvast.server.codekvast_server.config.CodekvastSettings;

import javax.inject.Inject;

/**
 * An MVC controller for all pages except the registration page.
 *
 * @author olle.hallin@crisp.se
 */
@Controller
public class WebController extends AbstractThymeleafController {

    @Inject
    private CodekvastSettings codekvastSettings;

    @RequestMapping({"/", "/index", "/page/**"})
    public String index() {
        return "index";
    }

    /**
     * If running in single-tenant (demo) mode, preload the guest  username and password.
     */
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
