/*
 * Copyright (c) 2015-2018 Hallin Information Technology AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.codekvast.login.api;

import io.codekvast.login.bootstrap.CodekvastLoginSettings;
import io.codekvast.login.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;

/**
 * @author olle.hallin@crisp.se
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final LoginService loginService;
    private final CodekvastLoginSettings settings;

    @ModelAttribute("settings")
    public CodekvastLoginSettings getCodekvastSettings() {
        return settings;
    }

    @GetMapping("/userinfo")
    public String userinfo(OAuth2AuthenticationToken authentication, Model model) {
        User user = loginService.getUserFromAuthentication(authentication);
        logger.info("User = {}", user);
        model.addAttribute("user", user);
        return "userinfo";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/index", "/home"})
    public String index(HttpServletRequest request, Authentication authentication) {
        logger.debug("index(): Request.contextPath={}", request.getContextPath());
        return authentication == null ? "index" : "redirect:userinfo";
    }

}
