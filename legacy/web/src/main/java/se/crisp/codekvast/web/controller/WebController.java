package se.crisp.codekvast.web.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import se.crisp.codekvast.web.service.VersionService;

import java.util.Date;

/**
 * A Spring MVC Controller that handles traditional HTTP requests from the Codekvast web UI.
 *
 *
 * @author olle.hallin@crisp.se
 */
@Controller
@Slf4j
public class WebController {
    @Autowired
    @NonNull
    @Value("${spring.thymeleaf.cache}")
    private Boolean thymeleafCache;

    @Autowired
    private VersionService versionService;

    private static final Date startedAt = new Date();

    @RequestMapping({"/", "/page/**"})
    public String index(ModelMap model) {
        model.put("thymeleafCache", thymeleafCache);

        String dot = thymeleafCache ? ".min." : ".";
        model.put("dotCss", dot + "css");
        model.put("dotJs", dot + "js");
        model.put("startedAt", startedAt);
        model.put("buildVersion", versionService.getFullBuildVersion());
        return "index";
    }
}
