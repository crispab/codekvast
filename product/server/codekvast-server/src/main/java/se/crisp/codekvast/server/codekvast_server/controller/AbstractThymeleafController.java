package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * A base class for traditional HTTP controllers which render Thymeleaf views.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public abstract class AbstractThymeleafController {

    @Autowired
    @NonNull
    @Value("${spring.thymeleaf.cache}")
    private Boolean thymeleafCache;

    /**
     * Stuff some common stuff into the MVC model...
     *
     * @param model
     */
    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("thymeleafCache", thymeleafCache);

        // If Thymeleaf caching is enabled, use minified versions of JS and CSS
        String dot = thymeleafCache ? ".min." : ".";
        model.addAttribute("dotCss", dot + "css");
        model.addAttribute("dotJs", dot + "js");
    }

}
