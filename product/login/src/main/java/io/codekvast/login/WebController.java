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
package io.codekvast.login;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

@Controller
@Slf4j
public class WebController {

    @Value("${application.version}")
    private String applicationVersion;

    @RequestMapping({"/", "/index", "/home"})
    String home(@CookieValue(name = "dummy", required = false) String dummyCookie,
                Model model) {
        logger.info("/index, cookies[dummy]={}, version={}", dummyCookie, applicationVersion);
        model.addAttribute("applicationVersion", applicationVersion);
        return "index";
    }

    @RequestMapping("/unauthenticated")
    public String unauthenticated() {
        logger.info("/unauthenticated");
        return "redirect:/?error=true";
    }

    @RequestMapping("/dummy")
    String dummy(@RequestHeader("Host") String hostHeader,
                 @CookieValue(name = "dummy", required = false) String dummyCookie,
                 HttpServletResponse response) {

        logger.debug("/dummy, request.header[Host]={}, cookies[dummy]={}", hostHeader, dummyCookie);
        response.addCookie(createDummyCookie(hostHeader));

        return "redirect:/";
    }

    private Cookie createDummyCookie(String hostHeader) {
        Cookie cookie = new Cookie("dummy", Instant.now().toString());
        cookie.setDomain(hostHeader.replaceAll(":[0-9]+$", ""));
        cookie.setPath("/");
        cookie.setMaxAge(-1);
        // cookie.setHttpOnly(true);
        return cookie;
    }

}
