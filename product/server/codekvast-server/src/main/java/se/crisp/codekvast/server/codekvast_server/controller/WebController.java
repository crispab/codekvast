package se.crisp.codekvast.server.codekvast_server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.inject.Inject;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
public class WebController extends AbstractThymeleafController {

    @Inject
    @Value("${codekvast.multi-tenant}")
    private boolean multiTenant;

    @RequestMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @RequestMapping({"/login"})
    public String login(Model model) {
        model.addAttribute("multiTenant", multiTenant);
        if (!multiTenant) {
            model.addAttribute("username", "guest");
            model.addAttribute("password", "0000");
        }
        return "login";
    }

}
