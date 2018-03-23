package io.codekvast.login.api;

import io.codekvast.login.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final LoginService loginService;

    @GetMapping("/userinfo")
    public String userinfo(OAuth2AuthenticationToken authentication) {

        User user = loginService.getUserFromAuthentication(authentication);
        logger.info("User = {}", user);
        return "userinfo.html";
    }

    @GetMapping("/customlogin")
    public String login() {
        return "login.html";
    }

}
