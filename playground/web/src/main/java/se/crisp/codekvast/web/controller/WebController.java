package se.crisp.codekvast.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private VersionService versionService;

    private static final Date startedAt = new Date();

    @RequestMapping({"/", "/page/**"})
    public String index(ModelMap model) {
        model.put("startedAt", startedAt);
        model.put("buildVersion", versionService.getFullBuildVersion());
        return "index";
    }
}
