package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * A base class for traditional HTTP controllers which render Thymeleaf views.
 *
 * @author olle.hallin@crisp.se
 */
@Slf4j
public abstract class AbstractThymeleafController {

    /**
     * Stuff some common stuff into the MVC model...
     *
     * @param model
     */
    @ModelAttribute
    public void populateModel(Model model) {
    }

}
