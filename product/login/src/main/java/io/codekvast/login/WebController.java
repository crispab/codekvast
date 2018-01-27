package io.codekvast.login;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
@RequestMapping(method = GET)
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final CodekvastLoginSettings settings;

    @ModelAttribute("janrainTokenUrl")
    public String getJanrainTokenUrl() {
        return settings.getJanrainTokenUrl();
    }

    @RequestMapping("/")
    String home() {
        logger.info("Welcome home.");
        return "home";
    }

    @RequestMapping("/feature1")
    String feature1() {
        return "feature1";
    }
}
