package se.crisp.codekvast.warehouse.web;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.inject.Inject;

/**
 * An MVC controller for all pages.
 *
 * @author olle.hallin@crisp.se
 */
@Controller
public class WebController {

    @Inject
    @NonNull
    @Value("${spring.thymeleaf.cache}")
    private Boolean thymeleafCache;

    /**
     * Stuff some common stuff into the MVC model...
     */
    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("thymeleafCache", thymeleafCache);

        // If Thymeleaf caching is enabled, use minified versions of JS and CSS
        String dot = thymeleafCache ? ".min." : ".";
        model.addAttribute("dotCss", dot + "css");
        model.addAttribute("dotJs", dot + "js");
        model.addAttribute("foo", "bar2");
    }


    @RequestMapping({"/", "/index", "/page/**"})
    public String index() {
        return "index";
    }

}
