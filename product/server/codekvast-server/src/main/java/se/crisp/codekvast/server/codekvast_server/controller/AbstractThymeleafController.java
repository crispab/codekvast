package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * A base class for traditional controllers which render Thymeleaf views.
 *
 * @author Olle Hallin
 */
@Slf4j
public abstract class AbstractThymeleafController {

    @Autowired
    @NonNull
    @Value("${spring.thymeleaf.cache}")
    private Boolean thymeleafCache;

    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("thymeleafCache", thymeleafCache);

        String dot = thymeleafCache ? ".min." : ".";
        model.addAttribute("dotCss", dot + "css");
        model.addAttribute("dotJs", dot + "js");
    }

}
