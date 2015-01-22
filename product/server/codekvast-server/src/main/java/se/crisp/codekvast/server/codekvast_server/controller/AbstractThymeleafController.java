package se.crisp.codekvast.server.codekvast_server.controller;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Spring MVC Controller that serves the webapp host page (index.html).
 *
 * @author Olle Hallin
 */
@Slf4j
public abstract class AbstractThymeleafController {

    @Autowired
    @NonNull
    @Value("${spring.thymeleaf.cache}")
    private Boolean thymeleafCache;

    private Map webjarVersions = new HashMap<>();

    @PostConstruct
    void scanWebjars() {
        Pattern pattern = Pattern.compile(".*webjars/(.*?)/(.*?)/.*");
        ClassLoader cl = getClass().getClassLoader();
        if (cl instanceof URLClassLoader) {
            URLClassLoader ucl = (URLClassLoader) cl;
            for (URL url : ucl.getURLs()) {
                Matcher matcher = pattern.matcher(url.toString());
                if (matcher.matches()) {
                    String key = matcher.group(1).replaceAll("[_-]", "").toLowerCase() + "Version";
                    String value = matcher.group(2);
                    webjarVersions.put(key, value);
                    log.debug("Found webjar {} {} in classpath", key, value);
                }
            }
        }
    }

    @ModelAttribute
    public void populateModel(Model model) {
        model.addAttribute("thymeleafCache", thymeleafCache);

        String dot = thymeleafCache ? ".min." : ".";
        model.addAttribute("dotCss", dot + "css");
        model.addAttribute("dotJs", dot + "js");
        model.addAllAttributes(webjarVersions);
    }

}
